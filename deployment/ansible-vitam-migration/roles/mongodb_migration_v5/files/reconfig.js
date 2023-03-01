// https://www.mongodb.com/docs/manual/release-notes/5.0-compatibility/#removed-customizable-values-for-getlasterrordefaults

cfg = rs.conf();
cfg.settings.getLastErrorDefaults = {w : 1, wtimeout: 0}
rs.reconfig(cfg)
