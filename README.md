# FURMS devops tools repsitory
This repository contains tools to package and install devops tools for FURMS project, as well as ansible scripts to install FURMS software itself.

# Content

## Installation tooling
The `install-tooling` directory contains a tool which is used to deploy FURMS devops tooling on target machine.

## Packaing tooling
The `pkg-tooling` directory contains script to build FURMS devops tooling bundle. The deliverables are created in `<repo>/target` directory.

### Create bundle
`<repo>/pkg-tooling/create-devops-pkg.py --bundle`
### Delete bundle
`<repo>/pkg-tooling/create-devops-pkg.py --clean`