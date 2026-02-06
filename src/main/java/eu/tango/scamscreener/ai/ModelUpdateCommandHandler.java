package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.ui.MessageDispatcher;

public final class ModelUpdateCommandHandler {
	private final ModelUpdateService modelUpdateService;

	public ModelUpdateCommandHandler(ModelUpdateService modelUpdateService) {
		this.modelUpdateService = modelUpdateService;
	}

	public int handleModelUpdateCommand(String action, String id) {
		return switch (action) {
			case "download" -> modelUpdateService.download(id, MessageDispatcher::reply);
			case "accept" -> modelUpdateService.accept(id, MessageDispatcher::reply);
			case "merge" -> modelUpdateService.merge(id, MessageDispatcher::reply);
			case "ignore" -> modelUpdateService.ignore(id, MessageDispatcher::reply);
			default -> 0;
		};
	}

	public int handleModelUpdateCheck(boolean force) {
		modelUpdateService.checkForUpdateAndDownloadAsync(MessageDispatcher::reply, force);
		return 1;
	}
}
