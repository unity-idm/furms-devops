# FURMS devops tools repsitory
This repository contains tools to package and install devops tools for FURMS project, as well as ansible scripts to install FURMS software itself.

# Content

## `pkg-tooling/create-devops-pkg.py`
FURMS devops tools packaging utility. This tool delivers package that is used to deploy devops tools with specific version on target machine.

### Create bundle for v0.0.1
    <repo>/pkg-tooling/create-devops-pkg.py --bundle 0.0.1
Creates the deliverables in `<repo>/target` directory (ignored by git) for specific version

### Delete bundle
    <repo>/pkg-tooling/create-devops-pkg.py --clean
Removes the `target` directory.


## `install-tooling/install-furms-devops-tooling.py`
FURMS devops tools installation utility. This utility is a part of a devops package, and its responsibility is to install
devops tools on a target machine. The script is prepared for a specific version, so whenever executed, it downloads packages
with fixed version and creates the neccessary directory structure.

### Installation
Assuming the script is available under `https://some_site_.io/0.0.1/install-furms-devops-tooling.py`, it can be used in one of the following ways:

* Directly using the url like `curl -sSL https://some_site_.io/0.0.1/install-furms-devops-tooling.py | python`, which will result in
package installation in current directory, or

* Download the script and run from command line to access some additional parameters:

```
usage: install-furms-devops-tooling.py [--install-dir path] [--version]

optional arguments:
--install-dir path  Install tooling under specified directory, not required,
                    current dir by default
--version           Print the version of devos tools to be installed by this
                    utility.
```

# Workflow
Assuming that devops scripts has been installed by aforementioned tool, the ansible deliverables are installed under `<install-dir>/furms-devops-tooling` directory,
Create the `<install-dir>/inventory` file along with secrets in `<install-dir>/group_vars/all.yml` configuration file. 
and now you can run ansible scripts e.g. to install FURMS:
```
cd <install-dir>
ansible-playbook -i inventory furms-devops-tooling/install-stack.yml 
```