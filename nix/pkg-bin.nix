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
      sha256 = "sha256-XB0ZVne0cOsPejkTuvLkZ7xknqMpmrqN2XJ3B3moyzY=";
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
