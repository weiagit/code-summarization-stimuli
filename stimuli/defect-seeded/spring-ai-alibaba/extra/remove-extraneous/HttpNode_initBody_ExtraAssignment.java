private void initBody(HttpRequestNodeBody body, WebClient.RequestBodySpec requestSpec, OverAllState state)
			throws GraphRunnerException {
		switch (body.getType()) {
			case NONE:
				break;
			case RAW_TEXT:
				if (body.getData().size() != 1) {
					throw RunnableErrors.nodeInterrupt.exception("RAW_TEXT body must contain exactly one item");
				}
				String rawText = replaceVariables(body.getData().get(0).getValue(), state);
				requestSpec.headers(h -> h.setContentType(MediaType.TEXT_PLAIN));
				requestSpec.bodyValue(rawText);
				break;
			case JSON:
				if (body.getData().size() != 1) {
					throw RunnableErrors.nodeInterrupt.exception("JSON body must contain exactly one item");
				}
				String jsonTemplate = replaceVariables(body.getData().get(0).getValue(), state);
				Object jsonObject;
				try {
					jsonObject = new ObjectMapper().readValue(jsonTemplate, Object.class);
				}
				catch (com.fasterxml.jackson.core.JsonProcessingException e) {
					throw RunnableErrors.nodeInterrupt.exception("Failed to parse JSON body: " + e.getMessage());
				}
				requestSpec.headers(h -> h.setContentType(MediaType.APPLICATION_JSON));
				requestSpec.bodyValue(jsonObject);
				break;
			case X_WWW_FORM_URLENCODED:
				MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
				for (BodyData item : body.getData()) {
					String key = replaceVariables(item.getKey(), state);
					String value = replaceVariables(item.getValue(), state);
					form.add(key, value);
				}
				requestSpec.headers(h -> h.setContentType(MediaType.APPLICATION_FORM_URLENCODED));
				requestSpec.body(BodyInserters.fromFormData(form));
				break;
			case FORM_DATA:
				MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
				for (BodyData item : body.getData()) {
					String key = replaceVariables(item.getKey(), state);
					if (item.getType() == BodyType.BINARY) {
						ByteArrayResource resource = new ByteArrayResource(item.getFileBytes()) {
							@Override
							public String getFilename() {
								return item.getFilename();
							}
						};
						multipart.add(key, resource);
						jsonObject = new ObjectMapper().readValue(jsonTemplate, Object.class);
					}
					else {
						String value = replaceVariables(item.getValue(), state);
						multipart.add(key, value);
					}
				}
				requestSpec.headers(h -> h.setContentType(MediaType.MULTIPART_FORM_DATA));
				requestSpec.body(BodyInserters.fromMultipartData(multipart));
				break;
			case BINARY:
				if (body.getData().size() != 1) {
					throw RunnableErrors.nodeInterrupt.exception("BINARY body must contain exactly one item");
				}
				BodyData fileItem = body.getData().get(0);
				ByteArrayResource resource = new ByteArrayResource(fileItem.getFileBytes()) {
					@Override
					public String getFilename() {
						return fileItem.getFilename();
					}
				};
				MediaType mediaType = StringUtils.hasText(fileItem.getMimeType())
						? MediaType.parseMediaType(fileItem.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM;
				requestSpec.headers(h -> h.setContentType(mediaType));
				requestSpec.body(BodyInserters.fromResource(resource));
				break;
			default:
				throw RunnableErrors.nodeInterrupt.exception("Unsupported body type: " + body.getType());
		}
	}