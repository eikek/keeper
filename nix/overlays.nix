{
  # Make sure sbt is run with jdk19
  sbt = self: super: {
    sbt = super.sbt.override {jre = super.openjdk19;};
  };

  # A simple startup script for postgresql running in foreground.
  postgres-fg = self: super: {
    postgres-fg = self.writeShellScriptBin "postgres-fg" ''
      data_dir="$1"
      port="''${2:-5432}"

      if [ -z "$data_dir" ]; then
          echo "A data directory is required!"
          exit 1
      fi

      if ! [ -f "$data_dir/PG_VERSION" ]; then
          echo "Initialize postgres cluster…"
          mkdir -p "$data_dir"
          chmod -R 700 "$data_dir"
          ${self.postgresql_16}/bin/pg_ctl init -D "$data_dir"
      fi

      ${self.postgresql_16}/bin/postgres -D "$data_dir" -k /tmp -h localhost -p $port
    '';
  };
}
