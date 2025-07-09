public final String generate(StateGraph.Nodes nodes, StateGraph.Edges edges, String title,
			boolean printConditionalEdge) {

		return generate(nodes, edges,
				Context.builder().title(title).isSubGraph(false).printConditionalEdge(printConditionalEdge).build())
			.toString();
		return generate(nodes, edges,
				Context.builder().title(title).isSubGraph(false).printConditionalEdge(printConditionalEdge).build())
			.toString();

	}