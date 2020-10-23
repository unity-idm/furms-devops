#!/usr/bin/env python
'''
Copyright (c) 2020 Bixbit s.c. All rights reserved.
See LICENSE file for licensing information.
'''

import sys, argparse, os
import urllib.request, tarfile

__version__ = "__set_me__"

class Env:
   __PACKAGE_HOST = "http://localhost:8000"

   PACKAGED_ANSIBLE_DIR_NAME = "furms-devops-tooling-%s" % __version__
   PACKAGE_NAME = "%s.tar.gz" % PACKAGED_ANSIBLE_DIR_NAME
   PACKAGE_URL = "%s/%s" % (__PACKAGE_HOST, PACKAGE_NAME)

def parse_arguments():
   parser = argparse.ArgumentParser(description='FURMS devops tools installation utility.')
   parser.add_argument('--install-dir', metavar='path', help='Install tooling under specified directory, \
         not required, current dir by default', action='store', default=os.getcwd())
   parser.add_argument('--version', help="Print the version of devos tools to be installed by this utility.",
      action='store_true')
   return parser.parse_args()

def download_package(target_file):
   urllib.request.urlretrieve(Env.PACKAGE_URL, target_file)

def unpack_archive(archive_file, unpacked_target_dir):
   tar = tarfile.open(archive_file, "r:gz")
   tar.extractall(unpacked_target_dir)

def create_link(unpacked_target_dir):
   pwd = os.getcwd()
   os.chdir(unpacked_target_dir)

   ansible_link = "furms-devops-tooling"
   ansible_dir = Env.PACKAGED_ANSIBLE_DIR_NAME
   if os.path.exists(ansible_link):
      os.unlink(ansible_link)
   os.symlink(ansible_dir, ansible_link)

   os.chdir(pwd)


def install_package(install_dir):
   print("Installation of FURMS devops tools v%s into %s" % (__version__, install_dir))
   os.makedirs(install_dir, exist_ok=True)

   compressed_target_file = os.path.join(install_dir, Env.PACKAGE_NAME)
   download_package(compressed_target_file)

   unpacked_target_dir = target_dir = os.path.dirname(os.path.abspath(compressed_target_file))
   unpack_archive(compressed_target_file, unpacked_target_dir)

   create_link(unpacked_target_dir)
   os.remove(compressed_target_file)

def main():
   args = parse_arguments()

   if args.version:
      print(__version__)
      exit(0)

   if args.install_dir:
      install_package(args.install_dir)

   print("Command finished successfully")

if __name__ == "__main__":
    main()
