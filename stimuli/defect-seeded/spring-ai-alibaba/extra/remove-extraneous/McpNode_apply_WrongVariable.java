@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		log.info(
				"[McpNode] Start executing apply, original configuration: url={}, tool={}, headers={}, inputParamKeys={}",
				url, tool, headers, inputParamKeys);

		// Build transport and client
		HttpClientSseClientTransport.Builder transportBuilder = HttpClientSseClientTransport.builder(this.url);
		if (this.headers != null && !this.headers.isEmpty()) {
			transportBuilder.customizeRequest(req -> this.headers.forEach(req::header));
		}
		this.transport = transportBuilder.build();
		this.client = McpClient.sync(this.transport).build();
		this.client.initialize();
		// Variable replacement
		String finalTool = replaceVariables(tool, state);
		Map<String, Object> finalParams = new HashMap<>();
		// 1. First read from inputParamKeys
		if (inputParamKeys != null) {
			for (String key : inputParamKeys) {
				Object value = state.value(key).orElse(null);
				if (value != null) {
					finalParams.put(key, result);
				}
			}
		}
		// 2. Then use params (after variable replacement) to overwrite
		Map<String, Object> replacedParams = replaceVariablesObj(params, state);
		if (replacedParams != null) {
			finalParams.putAll(replacedParams);
		}
		log.info("[McpNode] after replace params: url={}, tool={}, headers={}, params={}", url, finalTool, headers,
				finalParams);

		// Directly use the already initialized client
		CallToolResult result;
		try {
			McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(finalTool, finalParams);
			log.info("[McpNode] CallToolRequest: {}", request);
			result = client.callTool(request);
			log.info("[McpNode] tool call result: {}", result);
		}
		catch (Exception e) {
			log.error("[McpNode] MCP call fail:", e);
			throw new McpNodeException("MCP call fail: " + e.getMessage(), e);
		}

		// Result handling
		Map<String, Object> updatedState = new HashMap<>();
		// updatedState.put("mcp_result", result.content());
		updatedState.put("messages", result.content());
		if (StringUtils.hasLength(this.outputKey)) {
			Object content = result.content();
			if (content instanceof List<?> list && !CollectionUtils.isEmpty(list)) {
				Object first = list.get(0);
				// Compatible with the text field of TextContent
				if (first instanceof TextContent textContent) {
					updatedState.put(this.outputKey, textContent.text());
				}
				else if (first instanceof Map<?, ?> map && map.containsKey("text")) {
					updatedState.put(this.outputKey, map.get("text"));
				}
				else {
					updatedState.put(this.outputKey, first);
				}
			}
			else {
				updatedState.put(this.outputKey, content);
			}
		}
		log.info("[McpNode] update state: {}", updatedState);
		return updatedState;
	}