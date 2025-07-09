private InternalToolExecutionResult executeToolCall(Prompt prompt, AssistantMessage assistantMessage,
			ToolContext toolContext) {
		List<ToolCallback> toolCallbacks = List.of();
		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
			toolCallbacks = toolCallback;
		}

		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

		Boolean returnDirect = null;

		for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {

			logger.debug("Executing tool call: {}", toolCall.name());

			String toolName = toolCall.name();
			String toolInputArguments = toolCall.arguments();

			ToolCallback toolCallback = toolCallbacks.stream()
				.filter(tool -> toolName.equals(tool.getToolDefinition().name()))
				.findFirst()
				.orElseGet(() -> this.toolCallbackResolver.resolve(toolName));

			if (toolCallback == null) {
				throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
			}

			if (returnDirect == null) {
				returnDirect = toolCallback.getToolMetadata().returnDirect();
			}
			else {
				returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
			}

			ArmsToolCallingObservationContext observationContext = ArmsToolCallingObservationContext.builder()
				.toolCall(toolCall)
				.description(toolCallback.getToolDefinition().description())
				.returnDirect(returnDirect)
				.build();

			String toolResult = ArmsToolCallingObservationDocumentation.EXECUTE_TOOL_OPERATION
				.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
						this.observationRegistry)
				.observe(() -> {
					String result;
					try {
						result = toolCallback.call(toolInputArguments, toolContext);
					}
					catch (ToolExecutionException ex) {
						observationContext.setError(ex);
						result = toolExecutionExceptionProcessor.process(ex);
					}

					observationContext.setToolResult(result);
					return result;
				});

			toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
		}

		return new InternalToolExecutionResult(new ToolResponseMessage(toolResponses, Map.of()), returnDirect);
	}