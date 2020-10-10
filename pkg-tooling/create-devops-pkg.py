#!/usr/bin/env python
'''
BSD 2-Clause License

Copyright (c) 2020, Unity IdM
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
'''
import argparse
import sys, os, shutil, glob, tarfile

class Env:
    __PKG_TOOL_DIR_PATH = os.path.dirname(os.path.abspath(sys.argv[0]))
    __DEVOPS_REPO_PATH = os.path.dirname(__PKG_TOOL_DIR_PATH)

    TARGET_DIR_PATH = os.path.join(__DEVOPS_REPO_PATH, "target")
    ANSIBLE_DIR_PATH = os.path.join(__DEVOPS_REPO_PATH, 'ansible')
    INSTALL_TOOLING = os.path.join(__DEVOPS_REPO_PATH, 'install-tooling')

def parse_arguments():
    parser = argparse.ArgumentParser(description='FURMS devops tools packaging utility.')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--bundle', metavar='version', help='creates installation bundle with \
        specific version in target directory', action='store')
    group.add_argument('--clean', help='remove target directory', action='store_true')
    return parser.parse_args()

def make_tarfile(output_filename, source_dir):
    with tarfile.open(output_filename, "w:gz") as tar:
        tar.add(source_dir, arcname=os.path.basename(source_dir))

def create_ansible_deliverable(version):
    tar_ball = os.path.join(Env.TARGET_DIR_PATH, "ansible-%s.tar.gz" % version)
    make_tarfile(tar_ball, Env.ANSIBLE_DIR_PATH)

def copy_installation_script_to_target():
    '''
    Returns the name of the installation script.
    '''
    for filename in glob.glob(os.path.join(Env.INSTALL_TOOLING, '*.py')):
        shutil.copy(filename, Env.TARGET_DIR_PATH)
        return os.path.basename(filename)

def burn_out_version_in_script(installation_script_name, version):
    '''
    Parses the script and updates the __version__ inside.
    '''
    script_path = os.path.join(Env.TARGET_DIR_PATH, installation_script_name)
    fh = open(script_path, 'r+')
    file_content = fh.readlines()
    fh.close()

    for lineIdx, line in enumerate(file_content):
        if line.startswith("__version__"):
            file_content[lineIdx] = '__version__ = "%s"\n' % version
            break

    fh = open(script_path, 'w+')
    fh.writelines(file_content)
    fh.close()

def create_install_script_deliverable(version):
    '''
    Copies python script from install-tooling directory to target
    and parses it to change its version to specified one.
    '''
    installation_script_name = copy_installation_script_to_target()
    burn_out_version_in_script(installation_script_name, version)

def create_bundle(version):
    print("Creating devops installation bundle v%s" % version)
    if not os.path.exists(Env.TARGET_DIR_PATH):
        os.makedirs(Env.TARGET_DIR_PATH)
    
    create_install_script_deliverable(version)
    create_ansible_deliverable(version)

def clean_bundle():
    print("Removing target directory")
    if os.path.exists(Env.TARGET_DIR_PATH):
        shutil.rmtree(Env.TARGET_DIR_PATH)

def main():
    args = parse_arguments()

    if args.bundle:
        create_bundle(args.bundle)
    if args.clean:
        clean_bundle()

    print("Command finished successfully")

if __name__ == "__main__":
    main()