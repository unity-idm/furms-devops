#!/usr/bin/env python
'''
Copyright (c) 2020 Bixbit s.c. All rights reserved.
See LICENSE file for licensing information.
'''

import sys, argparse, os
import urllib.request, tarfile, requests

class GitHubAsset:
   def __init__(self, asset_download_url, asset_name):
      self._download_url = asset_download_url
      self._name = asset_name

   def name(self):
      return self._name
   def download_url(self):
      return self._download_url
   def copressed_dir_name(self):
      return self._name.replace('.tar.gz' ,'')

class GitHubClient:
   def __init__(self):
      resp = requests.get("https://api.github.com/repos/unity-idm/furms-devops/releases/latest")
      self._asset = resp.json()["assets"][0]
   def asset(self):
      return GitHubAsset(self._asset["browser_download_url"], self._asset["name"])

def parse_arguments():
   parser = argparse.ArgumentParser(description='FURMS devops tools installation utility.')
   parser.add_argument('--install-dir', metavar='path', help='Install tooling under specified directory, \
         not required, current dir by default', action='store', default=os.getcwd())
   parser.add_argument('--version', help="Print the version of devos tools to be installed by this utility.",
      action='store_true')
   return parser.parse_args()

def download_package(download_url, target_file):
   urllib.request.urlretrieve(download_url, target_file)

def unpack_archive(archive_file, unpacked_target_dir):
   tar = tarfile.open(archive_file, "r:gz")
   tar.extractall(unpacked_target_dir)

def create_link(unpacked_target_dir, copressed_dir_name):
   pwd = os.getcwd()
   os.chdir(unpacked_target_dir)

   ansible_link = "furms-devops-tooling"
   if os.path.exists(ansible_link):
      os.unlink(ansible_link)
   os.symlink(copressed_dir_name, ansible_link)

   os.chdir(pwd)


def install_package(install_dir, asset):
   print("Installation of latest FURMS devops tools into %s" % (install_dir))
   os.makedirs(install_dir, exist_ok=True)

   compressed_target_file = os.path.join(install_dir, asset.name())
   download_package(asset.download_url(), compressed_target_file)

   unpacked_target_dir = os.path.dirname(os.path.abspath(compressed_target_file))
   unpack_archive(compressed_target_file, unpacked_target_dir)

   create_link(unpacked_target_dir, asset.copressed_dir_name())
   os.remove(compressed_target_file)

def main():
   args = parse_arguments()

   asset = GitHubClient().asset()
   if args.version:
      print(asset.name())
      exit(0)

   if args.install_dir:
      install_package(args.install_dir, asset)

   print("Command finished successfully")

if __name__ == "__main__":
    main()
