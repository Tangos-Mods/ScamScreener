package eu.tango.scamscreener.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScamScreenerCommandsStructureTest {
	@Test
	void buildRootRegistersUploadAndRemovesAiTrain() throws Exception {
		ScamScreenerCommands commands = new ScamScreenerCommands(
			null,
			name -> new ResolvedTarget(UUID.randomUUID(), name),
			new MutePatternManager(),
			(playerName, label, count) -> 1,
			(messageId, label) -> 1,
			count -> 1,
			() -> 1,
			(action, id) -> 1,
			force -> 1,
			id -> 1,
			enabled -> {},
			(key, enabled) -> {},
			Map::of,
			() -> false,
			enabled -> {},
			() -> 1,
			() -> 1,
			() -> "",
			uuid -> {},
			() -> {},
			component -> {}
		);

		Method buildRoot = ScamScreenerCommands.class.getDeclaredMethod("buildRoot", String.class);
		buildRoot.setAccessible(true);
		@SuppressWarnings("unchecked")
		LiteralArgumentBuilder<FabricClientCommandSource> builder =
			(LiteralArgumentBuilder<FabricClientCommandSource>) buildRoot.invoke(commands, "scamscreener");

		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		dispatcher.register(builder);

		CommandNode<FabricClientCommandSource> root = dispatcher.getRoot().getChild("scamscreener");
		assertNotNull(root);
		assertNotNull(root.getChild("upload"));
		assertNotNull(root.getChild("ai"));
		assertNull(root.getChild("train"));

		CommandNode<FabricClientCommandSource> ai = root.getChild("ai");
		assertNull(ai.getChild("train"));
		assertNotNull(ai.getChild("reset"));
		assertTrue(ai.getChildren().stream().anyMatch(node -> "capture".equals(node.getName())));
	}
}
