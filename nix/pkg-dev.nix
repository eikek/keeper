{
  pkgs,
  lib,
  sbt,
}: let
  meta = (import ./meta.nix) lib;
in
  sbt.lib.mkSbtDerivation {
    inherit pkgs;
    inherit (meta) version;

    pname = "keeper-cli";

    nativeBuildInputs = [pkgs.nodejs pkgs.bash pkgs.sbt];

    src = lib.sourceByRegex ../. [
      "^build.sbt$"
      "^modules$"
      "^modules/.*$"
      "^project$"
      "^project/.*$"
      "^make-cli.sh$"
    ];

    depsSha256 = "sha256-8md2IIaX3PB/RM8/fA6rsw02hF2npQpkpJIZQPwUBEY=";

    buildPhase = ''
      env
      ls -lha
      bash make-cli.sh
    '';

    installPhase = ''
      mkdir -p $out
      cp -R modules/cli/target/universal/stage/* $out/

      cat > $out/bin/keeper <<-EOF
      #!${pkgs.bash}/bin/bash
      $out/bin/keeper-cli -java-home ${pkgs.openjdk} "\$@"
      EOF
      chmod 755 $out/bin/keeper
    '';
  }
