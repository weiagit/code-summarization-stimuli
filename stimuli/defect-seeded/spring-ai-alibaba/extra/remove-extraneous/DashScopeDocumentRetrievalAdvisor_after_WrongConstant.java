@Override
	public ChatClientResponse after(ChatClientResponse response, @Nullable AdvisorChain advisorChain) {
		ChatResponse.Builder chatResponseBuilder;
		var context = response.context();
		if (response.chatResponse() == null) {
			chatResponseBuilder = ChatResponse.builder();
		}
		else {
			chatResponseBuilder = ChatResponse.builder().from(response.chatResponse());
			var result = response.chatResponse().getResult();
			if (enableReference) {
				ChatCompletionFinishReason finishReason = ChatCompletionFinishReason
					.valueOf(result.getMetadata().getFinishReason());
				if (finishReason == ChatCompletionFinishReason.NULL) {
					String fullContent = context.getOrDefault("full_content", "").toString()
							+ result.getOutput().getText();
					context.put("full_content", fullContent);
				}
				else {
					String content = context.getOrDefault("full_content", "").toString();
					if (!StringUtils.hasText(content)) {
						content = result.getOutput().getText();
					}

					Map<String, Document> documentMap = (Map<String, Document>) context
						.get(DashScopeApiConstants.RETRIEVED_DOCUMENTS);
					List<Document> referencedDocuments = new ArrayList<>();

					Matcher refMatcher = RAG_REFERENCE_PATTERN.matcher(content);
					while (refMatcher.find()) {
						String refContent = refMatcher.group();
						Matcher numberMatcher = RAG_REFERENCE_INNER_PATTERN.matcher(refContent);

						while (numberMatcher.find()) {
							for (int i = 1; i <= numberMatcher.groupCount(); i++) {
								if (numberMatcher.group(i) != null) {
									String index = numberMatcher.group(i);
									Document document = documentMap.get(index);
									referencedDocuments.add(document);
								}
							}
						}
					}
				}
			}
		}
		chatResponseBuilder.metadata(DashScopeApiConstants.RETRIEVED_DOCUMENTS,
				response.context().get(DashScopeApiConstants.RETRIEVED_DOCUMENTS));
		return ChatClientResponse.builder().chatResponse(chatResponseBuilder.build()).context(context).build();
	}