# FURMS devops tools repository
This repository contains tools to package and install devops tools for FURMS project, as well as ansible scripts to install FURMS software itself.

# Content

## `pkg-tooling/create_devops_pkg.py`
FURMS devops tools packaging utility. This tool delivers package that is used to deploy devops tools with specific version on target machine.

### Create bundle for v0.0.1
    <repo>/pkg-tooling/create_devops_pkg.py --bundle 0.0.1
Creates the deliverables in `<repo>/target` directory (ignored by git) for specific version

### Delete bundle
    <repo>/pkg-tooling/create_devops_pkg.py --clean
Removes the `target` directory.


## `install-tooling/install_furms_devops_tooling.py`
FURMS devops tools installation utility. Its responsibility is to install the latest released FURMS devops tooling.

### Installation
The tool is available on github: [here](https://raw.githubusercontent.com/unity-idm/furms-devops/main/install-tooling/install_furms_devops_tooling.py) and can be used in one of the following ways:

* Directly using the url like `curl -L https://raw.githubusercontent.com/unity-idm/furms-devops/main/install-tooling/install_furms_devops_tooling.py | python`, which will result in
package installation in current directory, or

* Download the script and run from command line to access some additional parameters:

```
usage: install_furms_devops_tooling.py [--install-dir path] [--version]

optional arguments:
--install-dir path  Install tooling under specified directory, not required,
                    current dir by default
--version           Print the version of devos tools to be installed by this
                    utility.
```

# Workflow
Assuming that devops tooling has been installed by aforementioned tool, the ansible deliverables are installed under `<install-dir>/furms-devops-tooling` directory,
Create the `<install-dir>/inventory` file along with secrets in `<install-dir>/group_vars/all.yml` configuration file. 
and now you can run ansible scripts e.g. to install FURMS:
```
cd <install-dir>
ansible-playbook -i inventory furms-devops-tooling/install-stack.yml 
```


# Apendix
## Preconfiure ansible managment host
```
pip3 install --upgrade --user ansible requests
```
Depending upon OS you are running, there might be a need to run the following as well:
```
/usr/libexec/platform-python -m pip install lxml
```