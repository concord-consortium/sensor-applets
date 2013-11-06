package org.json;

import java.io.IOException;

public class JSONTest {
	public static void main(String[] args) throws IOException {
		JSONStringer str = new JSONStringer();
		str.array();
		  str.object().key("test");
		    str.array();
		      str.object().key("one").value(1).endObject();
		    str.endArray();
		  str.endObject();
		  str.object().key("arr").array().value(1).value(2).value(3).endArray().endObject();
		str.endArray();
		System.out.println(str.toString());
	}
}
