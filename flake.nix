{
  description = "keeper flake";
  inputs = {
    nixpkgs.url = "nixpkgs/nixos-24.05";
    devshell-tools.url = "github:eikek/devshell-tools";
    sbt.url = "github:zaninime/sbt-derivation";
    sbt.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = {
    self,
    nixpkgs,
    devshell-tools,
    sbt,
  }: let
    supportedSystems = ["x86_64-linux" "aarch64-linux" "x86_64-darwin"];
    forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
    keeperPkgs = pkgs: {
      keeper-dev = import ./nix/pkg-dev.nix {
        inherit pkgs;
        inherit sbt;
        lib = pkgs.lib;
      };
      keeper = pkgs.callPackage (import ./nix/pkg-bin.nix) {};
    };
  in rec
  {
    overlays.default = final: prev: (keeperPkgs final);

    formatter = forAllSystems (
      system:
        nixpkgs.legacyPackages.${system}.alejandra
    );

    apps = forAllSystems (system: {
      default = {
        type = "app";
        program = "${self.packages.${system}.default}/bin/keeper";
      };
    });

    packages = forAllSystems (system: let
      pkgs = import nixpkgs {
        inherit system;
        overlays = [self.overlays.default];
      };
    in {
      default = pkgs.keeper;
      keeper = pkgs.keeper;
      keeper-dev = pkgs.keeper-dev;
    });

    devShells = forAllSystems (system: {
      default = let
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
          nativeBuildInputs = [
          ];

          JAVA_HOME = "${pkgs.openjdk19}/lib/openjdk";
          KEEPER_POSTGRES_DATABASE = "keeper_dev";
          KEEPER_POSTGRES_USER = "dev";
          KEEPER_POSTGRES_PASSWORD = "dev";
          KEEPER_POSTGRES_DEBUG = "false";
          KEEPER_FIT4S_URI = "http://localhost:8181";
        };
    });
  };
}
