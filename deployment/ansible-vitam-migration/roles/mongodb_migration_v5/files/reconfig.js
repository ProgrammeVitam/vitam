cfg = rs.conf();
cfg.settings.getLastErrorDefaults = {w : 1, wtimeout: 0}
rs.reconfig(cfg)