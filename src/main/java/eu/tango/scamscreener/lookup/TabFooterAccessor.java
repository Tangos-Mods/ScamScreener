package eu.tango.scamscreener.lookup;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TabFooterAccessor {
	public Component readFooter(Minecraft client) {
		if (client.getConnection() == null) {
			return null;
		}

		Object connection = client.getConnection();
		String[] methodNames = {"getTabListFooter", "getTabListDisplayFooter", "getFooter"};
		for (String methodName : methodNames) {
			try {
				Method method = connection.getClass().getMethod(methodName);
				Object value = method.invoke(connection);
				if (value instanceof Component component) {
					return component;
				}
			} catch (Exception ignored) {
			}
		}

		String[] fieldNames = {"tabListFooter", "footer"};
		for (String fieldName : fieldNames) {
			try {
				Field field = connection.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				Object value = field.get(connection);
				if (value instanceof Component component) {
					return component;
				}
			} catch (Exception ignored) {
			}
		}

		return null;
	}
}
