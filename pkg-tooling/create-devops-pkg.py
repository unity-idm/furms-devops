#!/usr/bin/env python
'''
Copyright (c) 2020 Bixbit s.c. All rights reserved.
See LICENSE file for licensing information.
'''
import argparse
import sys, os, shutil, glob, tarfile

class Env:
    __PKG_TOOL_DIR_PATH = os.path.dirname(os.path.abspath(sys.argv[0]))
    __DEVOPS_REPO_PATH = os.path.dirname(__PKG_TOOL_DIR_PATH)

    TARGET_DIR_PATH = os.path.join(__DEVOPS_REPO_PATH, "target")
    ANSIBLE_DIR_PATH = os.path.join(__DEVOPS_REPO_PATH, 'furms-devops-tooling')
    INSTALL_TOOLING = os.path.join(__DEVOPS_REPO_PATH, 'install-tooling')

def parse_arguments():
    parser = argparse.ArgumentParser(description='FURMS devops tools packaging utility.')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--bundle', metavar='version', help='creates installation bundle with \
        specific version in target directory', action='store')
    group.add_argument('--clean', help='remove target directory', action='store_true')
    return parser.parse_args()

def remove_directory(dir):
    if os.path.exists(dir):
        shutil.rmtree(dir)

def make_tarfile(output_filename, source_dir):
    with tarfile.open(output_filename, "w:gz") as tar:
        tar.add(source_dir, arcname=os.path.basename(source_dir))

def create_ansible_deliverable(version):
    tar_ball = os.path.join(Env.TARGET_DIR_PATH, "furms-devops-tooling-%s.tar.gz" % version)
    ansible_content = os.path.join(Env.TARGET_DIR_PATH, "furms-devops-tooling-%s" % version)
    remove_directory(ansible_content)
    shutil.copytree(Env.ANSIBLE_DIR_PATH, ansible_content)
    make_tarfile(tar_ball, ansible_content)
    remove_directory(ansible_content)

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
    remove_directory(Env.TARGET_DIR_PATH)

def main():
    args = parse_arguments()

    if args.bundle:
        create_bundle(args.bundle)
    if args.clean:
        clean_bundle()

    print("Command finished successfully")

if __name__ == "__main__":
    main()