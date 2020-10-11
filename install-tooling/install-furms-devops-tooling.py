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

import sys, argparse, os
import urllib.request, tarfile

__version__ = "__set_me__"

class Env:
   __PACKAGE_HOST = "http://localhost:8000"

   PACKAGED_ANSIBLE_DIR_NAME = "ansible-%s" % __version__
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

   ansible_link = "ansible"
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
