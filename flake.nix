{
  description = "keeper flake";
  inputs = {
    nixpkgs.url = "nixpkgs/nixos-23.11";
    sbt.url = "github:zaninime/sbt-derivation";
    sbt.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = { self, nixpkgs, sbt }:
    let
      supportedSystems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" ];
      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
    in
    rec
    {
      overlays.default = final: prev: {
        keeper = with final; sbt.lib.mkSbtDerivation {
          pkgs = final;

          version = "dynamic";
          pname = "keeper-cli";

          nativeBuildInputs = [ pkgs.nodejs ];

          # depsWarmupCommand = ''
          #   sbt compile webviewJS/tzdbCodeGen
          # '';

          src = lib.sourceByRegex ./. [
            "^build.sbt$"
            "^modules$"
            "^modules/.*$"
            "^project$"
            "^project/.*$"
            "^make-cli.sh$"
          ];

          depsSha256 = "sha256-+Yj69e4d3o2Nf2kGvK6hmD7fpYyZAqAmoKxC3/10Kmk=";

          buildPhase = ''
            env
            ls -lha
            bash make-cli.sh
          '';

          installPhase = ''
            mkdir -p $out
            cp -R modules/cli/target/universal/stage/* $out/

            cat > $out/bin/keeper <<-EOF
            #!${bash}/bin/bash
            $out/bin/keeper-cli -java-home ${jdk} "\$@"
            EOF
            chmod 755 $out/bin/keeper
          '';
        };
      };

      apps = forAllSystems (system:
        { default = {
            type = "app";
            program = "${self.packages.${system}.default}/bin/keeper";
          };
        });

      packages = forAllSystems (system:
        {
          default = (import nixpkgs {
            inherit system;
            overlays = [ self.overlays.default ];
          }).keeper;
        });


      devShells = forAllSystems(system:
        { default =
            let
              overlays = import ./nix/overlays.nix;
              pkgs = import nixpkgs {
                inherit system;
                overlays = [
                  overlays.sbt
                  overlays.postgres-fg
                ];
              };
            in
              pkgs.mkShell {
                buildInputs = [
                  pkgs.sbt
                  pkgs.openjdk
                  pkgs.nodejs
                  pkgs.postgres-fg
                ];
                nativeBuildInputs =
                  [
                  ];

                JAVA_HOME = "${pkgs.openjdk19}/lib/openjdk";
                KEEPER_POSTGRES_DATABASE = "keeper_dev";
                KEEPER_POSTGRES_USER = "dev";
                KEEPER_POSTGRES_PASSWORD = "dev";
                KEEPER_POSTGRES_DEBUG = "false";
                KEEPER_FIT4S_URI = "http://fit4s.daheim.site";
              };
        });
    };
}
