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

class ScamScreenerCommandsStructureTest {
	@Test
	void buildRootRegistersUploadAndRemovesAiTrain() throws Exception {
		ScamScreenerCommands commands = new ScamScreenerCommands(
			null,
			null,
			name -> new ResolvedTarget(UUID.randomUUID(), name),
			new MutePatternManager(),
			(messageId, label) -> 1,
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
			() -> 1,
			() -> 1,
			() -> "",
			alertId -> 1,
			alertId -> 1,
			playerName -> 1,
			java.util.List::of,
			() -> 1,
			messageId -> 1,
			uuid -> {},
			() -> {},
			() -> {},
			component -> {}
		);

		Method buildRoot = ScamScreenerCommands.class.getDeclaredMethod("buildRoot", String.class);
		buildRoot.setAccessible(true);
		@SuppressWarnings("unchecked")
		LiteralArgumentBuilder<FabricClientCommandSource> builder =
			(LiteralArgumentBuilder<FabricClientCommandSource>) buildRoot.invoke(commands, "scamscreener");
		@SuppressWarnings("unchecked")
		LiteralArgumentBuilder<FabricClientCommandSource> aliasBuilder =
			(LiteralArgumentBuilder<FabricClientCommandSource>) buildRoot.invoke(commands, "ss");

		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		dispatcher.register(builder);
		dispatcher.register(aliasBuilder);

		CommandNode<FabricClientCommandSource> root = dispatcher.getRoot().getChild("scamscreener");
		assertNotNull(root);
		assertNotNull(root.getChild("help"));
		assertNotNull(root.getChild("upload"));
		assertNotNull(root.getChild("ai"));
		assertNotNull(root.getChild("whitelist"));
		assertNull(root.getChild("train"));

		CommandNode<FabricClientCommandSource> ai = root.getChild("ai");
		assertNull(ai.getChild("train"));
		assertNotNull(ai.getChild("reset"));
		assertNotNull(ai.getChild("metrics"));
		assertNotNull(ai.getChild("flag"));
		assertNull(ai.getChild("capture"));
		assertNull(ai.getChild("capturebulk"));

		CommandNode<FabricClientCommandSource> review = root.getChild("review");
		assertNotNull(review);
		assertNotNull(review.getChild("help"));
		assertNotNull(review.getChild("player"));

		CommandNode<FabricClientCommandSource> whitelist = root.getChild("whitelist");
		assertNotNull(whitelist);
		assertNotNull(whitelist.getChild("add"));
		assertNotNull(whitelist.getChild("remove"));

		CommandNode<FabricClientCommandSource> aliasRoot = dispatcher.getRoot().getChild("ss");
		assertNotNull(aliasRoot);
		assertNotNull(aliasRoot.getChild("help"));
		assertNotNull(aliasRoot.getChild("upload"));
		assertNotNull(aliasRoot.getChild("ai"));
		assertNotNull(aliasRoot.getChild("whitelist"));
		assertNull(aliasRoot.getChild("train"));
	}
}
