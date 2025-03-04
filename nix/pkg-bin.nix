{
  lib,
  stdenv,
  fetchzip,
  jdk17,
  unzip,
  bash,
}: let
  meta = (import ./meta.nix) lib;
  version = meta.latest-release;
in
  stdenv.mkDerivation {
    inherit version;
    name = "keeper-bin-${version}";

    src = fetchzip {
      url = "https://github.com/eikek/keeper/releases/download/v${version}/keeper-cli-${version}.zip";
      sha256 = "sha256-/45vBtoiEScIr69RxO7D0r6jPzMUeb8riTn22Bn79Ns=";
    };

    buildPhase = "true";

    installPhase = ''
      mkdir -p $out/{bin,keeper-${version}}
      cp -R * $out/keeper-${version}/
      cat > $out/bin/keeper <<-EOF
      #!${bash}/bin/bash
      $out/keeper-${version}/bin/keeper-cli -java-home ${jdk17} "\$@"
      EOF
      chmod 755 $out/bin/keeper
    '';

    meta = meta.meta-bin;
  }
