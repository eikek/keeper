lib: rec {
  version = "0.2.0-SNAPSHOT";

  latest-release = "0.1.0";

  license = lib.licenses.gpl3;
  homepage = https://github.com/eikek/keeper;

  meta-bin = {
    description = ''
      Keeper aims to help you keeping track of changes to your bikes.
    '';

    inherit license homepage;
  };

  meta-src = {
    description = ''
      Keeper aims to help you keeping track of changes to your bikes.
    '';

    inherit license homepage;
  };
}
