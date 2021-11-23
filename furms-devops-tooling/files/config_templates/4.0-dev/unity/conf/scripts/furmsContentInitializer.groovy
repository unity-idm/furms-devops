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
	upsertCommonAttrTypesFromResource()
	upsertAdminUser()
	upsertFurmsRestClient()
	upsertRegistrationForms()
	log.info("Database already initialized, configuraiton update...")
	return
} 

try
{
	upsertCommonAttrTypesFromResource()
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
	upsertFenixAdminRegistrationForm()
	upsertSiteUserRegistrationForm()
	upsertCommunityAdminRegistrationForm()
	upsertProjectUserRegistrationForm()
}

void upsertFenixAdminRegistrationForm()
{
	def fenixGroupParam = new GroupRegistrationParam()
	fenixGroupParam.setGroupPath("/fenix/users")

	def fenixRoleParam = new AttributeRegistrationParam()
	fenixRoleParam.setGroup('/fenix/users')
	fenixRoleParam.setAttributeType('furmsFenixRole')
	fenixRoleParam.setRetrievalSettings(ParameterRetrievalSettings.interactive)
	fenixRoleParam.setGroup('DYN:/fenix/users')

	upsertRegistrationForm("fenixForm", fenixGroupParam, fenixRoleParam)
}

void upsertSiteUserRegistrationForm()
{
	def siteGroupParam = new GroupRegistrationParam()
	siteGroupParam.setGroupPath("/fenix/sites/*/users")

	def siteRoleParam = new AttributeRegistrationParam()
	siteRoleParam.setGroup('/')
	siteRoleParam.setAttributeType('furmsSiteRole')
	siteRoleParam.setRetrievalSettings(ParameterRetrievalSettings.interactive)
	siteRoleParam.setGroup('DYN:/fenix/sites/*/users')

	upsertRegistrationForm("siteForm", siteGroupParam, siteRoleParam)
}

void upsertCommunityAdminRegistrationForm()
{
	def communityGroupParam = new GroupRegistrationParam()
	communityGroupParam.setGroupPath("/fenix/communities/*/users")

	def communityRoleParam = new AttributeRegistrationParam()
	communityRoleParam.setGroup('/')
	communityRoleParam.setAttributeType('furmsCommunityRole')
	communityRoleParam.setRetrievalSettings(ParameterRetrievalSettings.interactive)
	communityRoleParam.setGroup('DYN:/fenix/communities/*/users')

	upsertRegistrationForm("communityForm", communityGroupParam, communityRoleParam)
}

void upsertProjectUserRegistrationForm()
{
	def projectGroupParam = new GroupRegistrationParam()
	projectGroupParam.setGroupPath("/fenix/communities/*/projects/*/users")

	def projectRoleParam = new AttributeRegistrationParam()
	projectRoleParam.setGroup('/')
	projectRoleParam.setAttributeType('furmsProjectRole')
	projectRoleParam.setRetrievalSettings(ParameterRetrievalSettings.interactive)
	projectRoleParam.setGroup('DYN:/fenix/communities/*/projects/*/users')

	upsertRegistrationForm("projectForm", projectGroupParam, projectRoleParam)
}

private RegistrationForm upsertRegistrationForm(String name, 
		GroupRegistrationParam groupParam, 
		AttributeRegistrationParam roleParam) 
{
	def identityParam = new IdentityRegistrationParam()
	identityParam.setIdentityType('identifier')
	identityParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)

	def registrationFormNotifications = new RegistrationFormNotifications()
	registrationFormNotifications.setInvitationTemplate('registrationInvitation')

	def form = new RegistrationFormBuilder()
			.withName(name)
			.withPubliclyAvailable(true)
			.withByInvitationOnly(true)
			.withAutoLoginToRealm('main')
			.withDefaultCredentialRequirement("user password")
			.withNotificationsConfiguration(registrationFormNotifications)
			.withExternalSignupSpec(new ExternalSignupSpec([new AuthenticationOptionsSelector('registration', 'registration')]))
			.withDisplayedName(new I18nString("en", "Sign up to FURMS"))
			.withFormInformation(new I18nString("en", "You were invited to become \${custom.role} in FURMS."))
			.build()
	form.setIdentityParams([identityParam])

	form.setGroupParams([groupParam])

	def surnameParam = new AttributeRegistrationParam()
	surnameParam.setGroup('/')
	surnameParam.setAttributeType(SURNAME_ATTR)
	surnameParam.setOptional(true)
	surnameParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)

	def nameParam = new AttributeRegistrationParam()
	nameParam.setGroup('/')
	nameParam.setAttributeType(NAME_ATTR)
	nameParam.setOptional(true)
	nameParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)


	def firstnameParam = new AttributeRegistrationParam()
	firstnameParam.setGroup('/')
	firstnameParam.setAttributeType(FIRSTNAME_ATTR)
	firstnameParam.setOptional(true)
	firstnameParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)

	def emailParam = new AttributeRegistrationParam()
	emailParam.setGroup('/')
	emailParam.setAttributeType(EMAIL_ATTR)
	emailParam.setConfirmationMode(ConfirmationMode.CONFIRMED)
	emailParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden)

	form.setAttributeParams([
			roleParam, surnameParam, nameParam, firstnameParam, emailParam
	])
	form.setWrapUpConfig([
			new RegistrationWrapUpConfig(
					RegistrationWrapUpConfig.TriggeringState.DEFAULT,
					new I18nString('Your account has been created.'),
					new I18nString("You can log in now."),
					new I18nString("Continue"),
					true,
					'https://{{furmsServer.advertisedHost}}/public/registration',
					Duration.ofSeconds(5)
			),
			new RegistrationWrapUpConfig(
					RegistrationWrapUpConfig.TriggeringState.GENERAL_ERROR,
					new I18nString('Error'),
					new I18nString("Please contact with support."),
					new I18nString("Continue"),
					false,
					'https://{{furmsServer.advertisedHost}}/front/start/role/chooser',
					Duration.ZERO
			),
			new RegistrationWrapUpConfig(
					RegistrationWrapUpConfig.TriggeringState.PRESET_USER_EXISTS,
					new I18nString('You have already the account.'),
					new I18nString("You can log in and accept invitations."),
					new I18nString("Continue"),
					false,
					'https://{{furmsServer.advertisedHost}}/front/users/settings/invitations',
					Duration.ZERO
			)
	])
	form.setTranslationProfile(
			new TranslationProfile('registrationProfile', '', ProfileType.REGISTRATION, [
					new TranslationRule("true", new TranslationAction("autoProcess", ["accept"] as String[]))
			])
	)

	if (registrationsManagement.hasForm(form.getName()))
	{
		log.info("Updateing registration form: {}", form.getName());
		registrationsManagement.updateForm(form, true);
	} else
	{
		log.info("Creating new registration form: {}", form.getName());
		registrationsManagement.addForm(form);
	}
}

void upsertCommonAttrTypesFromResource() throws EngineException
{
	Map<String, AttributeType> existingAttrs = attributeTypeManagement.getAttributeTypesAsMap();
	List<Resource> resources = attributeTypeSupport.getAttibuteTypeResourcesFromClasspathDir()
	for (Resource r : resources)
		upsertAttrsFromFile(r, existingAttrs)
		
	Resource furmsAttributes = new FileSystemResource("conf/attributeTypes/" + COMMON_ATTR_FILE + ".json");
	if (furmsAttributes.exists())
		upsertAttrsFromFile(furmsAttributes, existingAttrs)
	
	log.info("Provisioned FURMS attribute types from resource")
}

void upsertAttrsFromFile(Resource r, Map<String, AttributeType> existingAttrs)
{
	if (FilenameUtils.getBaseName(r.getFilename()).equals(COMMON_ATTR_FILE))
	{
		List<AttributeType> attrTypes = attributeTypeSupport.loadAttributeTypesFromResource(r)
		for (AttributeType type : attrTypes)
		{
			if (existingAttrs.containsKey(type.getName()))
			{
				attributeTypeManagement.updateAttributeType(type)
				log.info("Updating attribute type: " + type)
			} else
			{
				attributeTypeManagement.addAttributeType(type)
				log.info("Adding attribute type: " + type)
			}
    	}
	}
	log.info("Common attributes upserted from resource file: " + r.getFilename())
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

	log.info("FURMS client user {}", (clientRestEntity == null) ? "created" : "updated")
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

void upsertAdminUser() throws EngineException
{
	String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER)
	EntityParam entity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, adminU))
	
	Attribute emailA = VerifiableEmailAttribute.of(EMAIL_ATTR, "/", "admin@not-existing-1qaz.example.com")
	attributesManagement.setAttribute(entity, emailA)
}

void setupAdminUser() throws EngineException
{
	String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER)
	EntityParam entity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, adminU))
	
	Attribute nameA = StringAttribute.of(NAME_ATTR, "/", "Default FENIX Administrator")
	attributesManagement.createAttribute(entity, nameA)
	
	Attribute emailA = VerifiableEmailAttribute.of(EMAIL_ATTR, "/", "admin@not-existing-1qaz.example.com")
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

