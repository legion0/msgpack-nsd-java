package org.msgpack.nsd;

import org.msgpack.type.Value;

public interface ResponseReader {
	public void read(Value extra_info);
}
