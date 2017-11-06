package de.bguenthe.dailytasks;

import io.flic.lib.FlicManager;

/**
 * Created by Emil on 2015-11-30.
 */
public class Config {
	static void setFlicCredentials() {
		FlicManager.setAppCredentials("dailytasks", "ef14b596-72bb-4fa0-898a-872d200fd3fc", "dailytasks");
	}
}
