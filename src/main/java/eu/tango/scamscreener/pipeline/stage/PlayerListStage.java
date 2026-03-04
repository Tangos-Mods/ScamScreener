package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.Whitelist;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

import java.util.Objects;

/**
 * Early pipeline stage that applies whitelist and blacklist checks.
 */
public final class PlayerListStage extends Stage {
    private final Whitelist whitelistList;
    private final Blacklist blacklistList;

    /**
     * Creates the stage with explicit list instances.
     *
     * @param whitelistList the whitelist to check first
     * @param blacklistList the blacklist to check second
     */
    public PlayerListStage(Whitelist whitelistList, Blacklist blacklistList) {
        this.whitelistList = Objects.requireNonNull(whitelistList, "whitelistList");
        this.blacklistList = Objects.requireNonNull(blacklistList, "blacklistList");
    }

    /**
     * Evaluates player-list checks for the current sender.
     *
     * @param chatEvent the chat event received from the client
     * @return a terminal whitelist/blacklist result, or pass when no match exists
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        if (whitelistList.contains(chatEvent.getSenderUuid(), chatEvent.getSenderName())) {
            // Whitelist wins first because it is the strongest explicit trust override.
            return whitelist("WHITELIST_MATCH", "player_list.whitelist_match");
        }

        if (blacklistList.contains(chatEvent.getSenderUuid(), chatEvent.getSenderName())) {
            // Blacklist is an explicit deny-list decision, distinct from heuristic blocking.
            return blacklist("BLACKLIST_MATCH", "player_list.blacklist_match");
        }

        return pass();
    }
}
