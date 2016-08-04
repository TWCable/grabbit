#!/usr/bin/env sh

PROTOC_VERSION="2.6.1"

if [[ ! -d "$HOME/protobuf/lib" ||  "$(protoc --version)" != *${PROTOC_VERSION}* ]]; then
  pushd .
  cd /tmp
  wget https://github.com/google/protobuf/releases/download/v${PROTOC_VERSION}/protobuf-${PROTOC_VERSION}.tar.gz
  tar -xzvf protobuf-${PROTOC_VERSION}.tar.gz
  cd protobuf-${PROTOC_VERSION}
  ./configure --prefix=$HOME/protobuf
  make
  make check
  make install
  cd ..
  rm -rf protobuf-${PROTOC_VERSION}
  rm -f protobuf-${PROTOC_VERSION}.zip
  popd
else
  echo "$(protoc --version) already installed"
fi
