CREATE UNIQUE INDEX organization_options_kind_name_case_insensitive_key
    ON organization_options (kind, lower(name));
