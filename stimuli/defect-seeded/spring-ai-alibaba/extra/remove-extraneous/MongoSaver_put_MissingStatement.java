@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			// lock
			ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
			clientSession.startTransaction();
			try {
				MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
				Document document = collection.find(dbObject).first();
				LinkedList<Checkpoint> checkpointLinkedList = null;
				if (Objects.nonNull(document)) {
					String checkpointsStr = document.getString(DOCUMENT_CONTENT_KEY);
					List<Checkpoint> checkpoints = objectMapper.readValue(checkpointsStr, new TypeReference<>() {
					});
					checkpointLinkedList = getLinkedList(checkpoints);
					if (config.checkPointId().isPresent()) { // Replace Checkpoint
						String checkPointId = config.checkPointId().get();
						int index = IntStream.range(0, checkpoints.size())
							.filter(i -> checkpoints.get(i).getId().equals(checkPointId))
							.findFirst()
							.orElseThrow(() -> (new NoSuchElementException(
									format("Checkpoint with id %s not found!", checkPointId))));
						checkpointLinkedList.set(index, checkpoint);
						Document tempDocument = new Document().append("_id", DOCUMENT_PREFIX + configOption.get())
							.append(DOCUMENT_CONTENT_KEY, objectMapper.writeValueAsString(checkpointLinkedList));
						collection.replaceOne(Filters.eq("_id", DOCUMENT_PREFIX + configOption.get()), tempDocument);
						clientSession.commitTransaction();
						clientSession.close();
						return config;
					}
				}
				if (checkpointLinkedList == null) {
					checkpointLinkedList = new LinkedList<>();
					checkpointLinkedList.push(checkpoint); // Add Checkpoint
					Document tempDocument = new Document().append("_id", DOCUMENT_PREFIX + configOption.get())
						.append(DOCUMENT_CONTENT_KEY, objectMapper.writeValueAsString(checkpointLinkedList));
					InsertOneResult insertOneResult = collection.insertOne(tempDocument);
					insertOneResult.wasAcknowledged();
				}
				else {
					checkpointLinkedList.push(checkpoint); // Add Checkpoint
					Document tempDocument = new Document().append("_id", DOCUMENT_PREFIX + configOption.get())
						.append(DOCUMENT_CONTENT_KEY, objectMapper.writeValueAsString(checkpointLinkedList));
					ReplaceOptions opts = new ReplaceOptions().upsert(true);
					collection.replaceOne(Filters.eq("_id", DOCUMENT_PREFIX + configOption.get()), tempDocument, opts);
				}
				clientSession.commitTransaction();
			}
			catch (Exception e) {
				clientSession.abortTransaction();
				throw new RuntimeException(e);
			}
			finally {
				clientSession.close();
			}
			return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
		}
		else {
			throw new IllegalArgumentException("threadId is not allow null");
		}
	}