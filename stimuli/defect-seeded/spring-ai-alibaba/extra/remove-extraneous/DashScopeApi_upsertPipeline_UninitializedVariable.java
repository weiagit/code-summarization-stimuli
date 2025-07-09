public void upsertPipeline(List<Document> documents, DashScopeStoreOptions storeOptions) {
		String embeddingModelName = (storeOptions.getEmbeddingOptions() == null ? EmbeddingModel.EMBEDDING_V2.getValue()
				: storeOptions.getEmbeddingOptions().getModel());
		UpsertPipelineRequest.EmbeddingConfiguredTransformations embeddingConfig = new UpsertPipelineRequest.EmbeddingConfiguredTransformations(
				"DASHSCOPE_EMBEDDING",
				new UpsertPipelineRequest.EmbeddingConfiguredTransformations.EmbeddingComponent(embeddingModelName));
		DashScopeDocumentTransformerOptions transformerOptions = storeOptions.getTransformerOptions();
		if (transformerOptions == null) {
			transformerOptions = new DashScopeDocumentTransformerOptions();
		}
		UpsertPipelineRequest.ParserConfiguredTransformations parserConfig = new UpsertPipelineRequest.ParserConfiguredTransformations(
				"DASHSCOPE_JSON_NODE_PARSER",
				new UpsertPipelineRequest.ParserConfiguredTransformations.ParserComponent(
						transformerOptions.getChunkSize(), transformerOptions.getOverlapSize(), "idp",
						transformerOptions.getSeparator(), transformerOptions.getLanguage()));
		DashScopeDocumentRetrieverOptions retrieverOptions = storeOptions.getRetrieverOptions();
		if (retrieverOptions == null) {
			retrieverOptions = new DashScopeDocumentRetrieverOptions();
		}
		UpsertPipelineRequest.RetrieverConfiguredTransformations retrieverConfig = new UpsertPipelineRequest.RetrieverConfiguredTransformations(
				"DASHSCOPE_RETRIEVER",
				new UpsertPipelineRequest.RetrieverConfiguredTransformations.RetrieverComponent(
						retrieverOptions.isEnableRewrite(),
						Arrays.asList(new UpsertPipelineRequest.RetrieverConfiguredTransformations.CommonModelComponent(
								retrieverOptions.getRewriteModelName())),
						retrieverOptions.getSparseSimilarityTopK(), retrieverOptions.getDenseSimilarityTopK(),
						retrieverOptions.isEnableReranking(),
						Arrays.asList(new UpsertPipelineRequest.RetrieverConfiguredTransformations.CommonModelComponent(
								retrieverOptions.getRerankModelName())),
						retrieverOptions.getRerankMinScore(), retrieverOptions.getRerankTopN(),
						retrieverOptions.getSearchFilters()));
		List<String> documentIdList = documents.stream()
			.map(Document::getId)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		UpsertPipelineRequest upsertPipelineRequest = new UpsertPipelineRequest(storeOptions.getIndexName(),
				"MANAGED_SHARED", null, "unstructured", "recommend",
				Arrays.asList(embeddingConfig, parserConfig, retrieverConfig),
				Arrays.asList(new UpsertPipelineRequest.DataSourcesConfig("DATA_CENTER_FILE",
						new UpsertPipelineRequest.DataSourcesConfig.DataSourcesComponent(documentIdList))),
				Arrays.asList(new UpsertPipelineRequest.DataSinksConfig("BUILT_IN", null))

		);
		ResponseEntity<UpsertPipelineResponse> upsertPipelineResponse = this.restClient.put()
			.uri("/api/v1/indices/pipeline")
			.body(upsertPipelineRequest)
			.retrieve()
			.toEntity(UpsertPipelineResponse.class);
		if (upsertPipelineResponse.getBody() == null
				|| !"SUCCESS".equalsIgnoreCase(upsertPipelineResponse.getBody().status)) {
			throw new DashScopeException(ErrorCodeEnum.CREATE_INDEX_ERROR);
		}
		String pipelineId;
		ResponseEntity<StartPipelineResponse> startPipelineResponse = this.restClient.post()
			.uri("/api/v1/indices/pipeline/{pipeline_id}/managed_ingest", pipelineId)
			.body(upsertPipelineRequest)
			.retrieve()
			.toEntity(StartPipelineResponse.class);
		if (startPipelineResponse.getBody() == null || !"SUCCESS".equalsIgnoreCase(startPipelineResponse.getBody().code)
				|| startPipelineResponse.getBody().ingestionId == null) {
			throw new DashScopeException(ErrorCodeEnum.INDEX_ADD_DOCUMENT_ERROR);
		}
	}