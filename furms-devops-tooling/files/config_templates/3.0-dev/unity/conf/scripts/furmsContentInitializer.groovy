import com.google.common.collect.Lists
import groovy.transform.Field
import org.apache.commons.io.FilenameUtils
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration
import pl.edu.icm.unity.exceptions.EngineException
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider
import pl.edu.icm.unity.stdext.attr.*
import pl.edu.icm.unity.types.I18nString
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider
import pl.edu.icm.unity.stdext.utils.ContactMobileMetadataProvider
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider
import pl.edu.icm.unity.types.basic.*
import pl.edu.icm.unity.types.registration.*
import pl.edu.icm.unity.types.translation.ProfileType
import pl.edu.icm.unity.types.translation.TranslationAction
import pl.edu.icm.unity.types.translation.TranslationProfile
import pl.edu.icm.unity.types.translation.TranslationRule

import java.time.Duration

@Field final String NAME_ATTR = "name"
@Field final String EMAIL_ATTR = "email"
@Field final String FIRSTNAME_ATTR = "firstname"
@Field final String SURNAME_ATTR = "surname"
@Field final String COMMON_ATTR_FILE = "furmsAttributes"

@Field final String ALLOWED_RETURN_URI_1 = "https://{{furmsServer.advertisedHost}}/login/oauth2/code/unity"

@Field final String FURMS_API_USERNAME = "{{unityApiClientUsername}}"
@Field final String FURMS_API_PASSWORD = "{{unityApiClientPassword}}"

@Field final String FURMS_OAUTH_USERNAME = "{{unityOauthClientUsername}}"
@Field final String FURMS_OAUTH_PASSWORD = "{{unityOauthClientPassword}}"

//run only if it is the first start of the server on clean DB.
if (!isColdStart)
{
	upsertFurmsRestClient()
	upsertRegistrationForms()
	log.info("Database already initialized, configuraiton update...")
	return
} 

try
{
	initCommonAttrTypesFromResource()
	initDefaultAuthzPolicy()
	initBaseGroups()
	setupAdminUser()
	initOAuthClient()
	upsertFurmsRestClient()
	upsertRegistrationForms()

} catch (Exception e)
{
	log.warn("Error loading data", e)
}

void upsertRegistrationForms()
{
	RegistrationForm fenixAdminForm = createRegistrationForm()
	fenixAdminForm.setTranslationProfile(
			new TranslationProfile('registrationProfile', '', ProfileType.REGISTRATION, [
					new TranslationRule("true", new TranslationAction("autoProcess", ["accept"] as String[])),
					new TranslationRule("true", new TranslationAction("addToGroup", "'/fenix/users'")),
					new TranslationRule("true", new TranslationAction("addAttribute", ["furmsFenixRole", "/fenix/users", "['ADMIN']"] as String[]))
			])
	)
	if (registrationsManagement.hasForm(fenixAdminForm.getName()))
	{
		log.info("Updateing registration form: {}", fenixAdminForm.getName());
		registrationsManagement.updateForm(fenixAdminForm, true);
	} else
	{
		log.info("Creating new registration form:", fenixAdminForm.getName());
		registrationsManagement.addForm(fenixAdminForm);
	}
}

private RegistrationForm createRegistrationForm() {
	def identityParam = new IdentityRegistrationParam()
	identityParam.setIdentityType('identifier')
	identityParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)

	def registrationFormNotifications = new RegistrationFormNotifications()
	registrationFormNotifications.setInvitationTemplate('registrationInvitation')

	def form = new RegistrationFormBuilder()
			.withName("fenixAdminForm")
			.withPubliclyAvailable(true)
			.withByInvitationOnly(true)
			.withAutoLoginToRealm('main')
			.withDefaultCredentialRequirement("user password")
			.withNotificationsConfiguration(registrationFormNotifications)
			.withExternalSignupSpec(new ExternalSignupSpec([new AuthenticationOptionsSelector('registration', 'registration')]))
			.build()
	form.setIdentityParams([identityParam])

	def surnameParam = new AttributeRegistrationParam()
	surnameParam.setGroup('/')
	surnameParam.setAttributeType('surname')
	surnameParam.setOptional(true)
	surnameParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)

	def nameParam = new AttributeRegistrationParam()
	nameParam.setGroup('/')
	nameParam.setAttributeType('name')
	nameParam.setOptional(true)
	nameParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)


	def firstnameParam = new AttributeRegistrationParam()
	firstnameParam.setGroup('/')
	firstnameParam.setAttributeType('firstname')
	firstnameParam.setOptional(true)
	firstnameParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)

	def emailParam = new AttributeRegistrationParam()
	emailParam.setGroup('/')
	emailParam.setAttributeType('email')
	emailParam.setConfirmationMode(ConfirmationMode.CONFIRMED)
	emailParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)

	form.setAttributeParams([
			surnameParam, nameParam, firstnameParam, emailParam
	])
	form.setWrapUpConfig([
			new RegistrationWrapUpConfig(
					RegistrationWrapUpConfig.TriggeringState.DEFAULT,
					new I18nString('Your account has been created.'),
					new I18nString("You can log in now."),
					new I18nString("Continue"),
					true,
					'https://localhost:3443/public/registration',
					Duration.ofSeconds(5)
			),
			new RegistrationWrapUpConfig(
					RegistrationWrapUpConfig.TriggeringState.GENERAL_ERROR,
					new I18nString('Error'),
					new I18nString("Please contact with support."),
					new I18nString("Continue"),
					false,
					'https://localhost:3443/front/start/role/chooser',
					Duration.ZERO
			),
			new RegistrationWrapUpConfig(
					RegistrationWrapUpConfig.TriggeringState.PRESET_USER_EXISTS,
					new I18nString('You have already the account.'),
					new I18nString("You can log in and accept invitations."),
					new I18nString("Continue"),
					false,
					'https://localhost:3443/front/users/settings/invitations',
					Duration.ZERO
			)
	])
	form
}

void initCommonAttrTypesFromResource() throws EngineException
{
	List<Resource> resources = attributeTypeSupport.getAttibuteTypeResourcesFromClasspathDir()
	for (Resource r : resources)
		loadAttrsFromFile(r)
		
	Resource furmsAttributes = new FileSystemResource("conf/attributeTypes/" + COMMON_ATTR_FILE + ".json");
	if (furmsAttributes.exists())
		loadAttrsFromFile(furmsAttributes)
	
	log.info("Provisioned FURMS attribute types from resource")
}

void loadAttrsFromFile(Resource r)
{
	if (FilenameUtils.getBaseName(r.getFilename()).equals(COMMON_ATTR_FILE))
	{
		List<AttributeType> attrTypes = attributeTypeSupport.loadAttributeTypesFromResource(r)
		for (AttributeType type : attrTypes)
		{
			attributeTypeManagement.addAttributeType(type)
			log.info("Addin attribute type: " + type)
    	}
	}
	log.info("Common attributes added from resource file: " + r.getFilename())
}

void upsertFurmsRestClient()
{
	Entity clientRestEntity = getEntityWithUsernameIdentity(FURMS_API_USERNAME)

	EntityParam entity;
	if (clientRestEntity == null)
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, FURMS_API_USERNAME)

		Identity base = entityManagement.addEntity(toAdd, EntityState.valid)
		entity = new EntityParam(base.getEntityId())

		Attribute role = EnumAttribute.of("sys:AuthorizationRole", "/", "Contents Manager")
		attributesManagement.createAttribute(entity, role)

		Attribute name = StringAttribute.of(NAME_ATTR, "/", "FURMS client user")
		attributesManagement.createAttribute(entity, name)

		PasswordToken clientPassword = new PasswordToken(FURMS_API_PASSWORD)
		entityCredentialManagement.setEntityCredential(entity, "clientPassword", clientPassword.toJson())
	} else
	{
		entity = new EntityParam(clientRestEntity.getId())
	}

	Attribute role = EnumAttribute.of("sys:AuthorizationRole", "/", "System Manager")
	attributesManagement.setAttribute(entity, role)

	log.info("FURMS client user {}}", (clientRestEntity == null) ? "created" : "updated")
}

Entity getEntityWithUsernameIdentity(String username)
{
	EntityParam userParam = new EntityParam(new IdentityTaV(UsernameIdentity.ID, username));
	try
	{
		return entityManagement.getEntity(userParam);
	} catch (pl.edu.icm.unity.exceptions.UnknownIdentityException e)
	{
		return null;
	}
}

void initDefaultAuthzPolicy() throws EngineException
{
	//create attribute statement for the root group, which assigns regular user role
	//to all its members
	AttributeStatement everybodyStmt = AttributeStatement.getFixedEverybodyStatement(
			EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User"))
	Group rootGroup = groupsManagement.getContents("/", GroupContents.METADATA).getGroup()
	AttributeStatement[] statements = [everybodyStmt]
	rootGroup.setAttributeStatements(statements)
	groupsManagement.updateGroup("/", rootGroup)
	log.info("Provisioned default FURMS authorization policy")
}


void setupAdminUser() throws EngineException
{
	String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER)
	EntityParam entity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, adminU))
	
	Attribute nameA = StringAttribute.of(NAME_ATTR, "/", "Default FENIX Administrator")
	attributesManagement.createAttribute(entity, nameA)
	
	Attribute emailA = VerifiableEmailAttribute.of(EMAIL_ATTR, "/", "admin@domain.com")
	attributesManagement.createAttribute(entity, emailA)

	Attribute firstnameA = StringAttribute.of(FIRSTNAME_ATTR, "/", "Default")
	attributesManagement.createAttribute(entity, firstnameA)

	Attribute surnameA = StringAttribute.of(SURNAME_ATTR, "/", "FENIX Admin")
	attributesManagement.createAttribute(entity, surnameA)

	String adminP = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_PASSWORD)
	PasswordToken pToken = new PasswordToken(adminP)
	entityCredentialManagement.setEntityCredential(entity, "userPassword", pToken.toJson())
	
	groupsManagement.addMemberFromParent("/fenix", entity);
	groupsManagement.addMemberFromParent("/fenix/users", entity);
	
	Attribute furmsFenixRole = EnumAttribute.of("furmsFenixRole", "/fenix/users", "ADMIN");
	attributesManagement.setAttribute(entity, furmsFenixRole, false);
}

void initBaseGroups()
{
	groupsManagement.addGroup(new Group("/fenix"))
	groupsManagement.addGroup(new Group("/fenix/users"))
	groupsManagement.addGroup(new Group("/fenix/sites"))
	groupsManagement.addGroup(new Group("/fenix/communities"))
	log.info("Provisioned base Furms groups")
}

void initOAuthClient()
{
	groupsManagement.addGroup(new Group("/oauth-clients"))
	IdentityParam oauthClient = new IdentityParam(UsernameIdentity.ID, FURMS_OAUTH_USERNAME)
	Identity oauthClientA = entityManagement.addEntity(oauthClient,
			EntityState.valid)
	PasswordToken pToken2 = new PasswordToken(FURMS_OAUTH_PASSWORD)

	EntityParam entityP = new EntityParam(oauthClientA.getEntityId())
	entityCredentialManagement.setEntityCredential(entityP, "userPassword", pToken2.toJson())
	log.warn("Furms OAuth client user was created.")

	Attribute cnA = StringAttribute.of(NAME_ATTR, "/", "OAuth client")
	attributesManagement.createAttribute(entityP, cnA)

	groupsManagement.addMemberFromParent("/oauth-clients", entityP)
	Attribute flowsA = EnumAttribute.of(OAuthSystemAttributesProvider.ALLOWED_FLOWS,
			"/oauth-clients",
			Lists.newArrayList(
					OAuthSystemAttributesProvider.GrantFlow.authorizationCode.toString(), OAuthSystemAttributesProvider.GrantFlow.implicit.toString(),
					OAuthSystemAttributesProvider.GrantFlow.openidHybrid.toString()))
	attributesManagement.createAttribute(entityP, flowsA)
	Attribute returnUrlA = StringAttribute.of(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI,
			"/oauth-clients",
			ALLOWED_RETURN_URI_1
	)
	attributesManagement.createAttribute(entityP, returnUrlA)
	log.info("Initialized all data required for oAuth2 client")
}

