package org.rrd4j.backend.spi.binary;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.rrd4j.backend.RrdBackend;
import org.rrd4j.backend.RrdBackendFactory;
import org.rrd4j.backend.RrdBackendMeta;
import org.rrd4j.core.RrdDb;

/**
 * Factory class which creates actual {@link RrdMemoryBackend} objects. Rrd4j's support
 * for in-memory RRDs is still experimental. You should know that all active RrdMemoryBackend
 * objects are held in memory, each backend object stores RRD data in one big byte array. This
 * implementation is therefore quite basic and memory hungry but runs very fast.<p>
 *
 * <p>Calling {@link RrdDb#close() close()} on RrdDb objects does not release any memory at all
 * (RRD data must be available for the next <code>new RrdDb(path)</code> call. To release allocated
 * memory, you'll have to call {@link #delete(java.lang.String) delete(path)} method of this class or stop the factory.</p>
 */
@RrdBackendMeta("MEMORY")
public class RrdMemoryBackendFactory extends RrdBackendFactory {
    
    final static class ByteBuffer {
        byte[] buffer;
        ByteBuffer(byte[] buffer) {
            this.buffer = buffer;
        }
        ByteBuffer() {
            this.buffer = null;
        }
    }
    
    protected  Map<String, ByteBuffer> backends;

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStart()
     */
    @Override
    protected boolean startBackend() {
        backends = new ConcurrentHashMap<String, ByteBuffer>();
        return true;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStop()
     */
    @Override
    protected boolean stopBackend() {
        backends.clear();
        backends = null;
        return true;
    }

    /**
     * Creates RrdMemoryBackend object.
     *
     * @param id       Since this backend holds all data in memory, this argument is interpreted
     *                 as an ID for this memory-based storage.
     * @param readOnly This parameter is ignored
     * @return RrdMemoryBackend object which handles all I/O operations
     * @throws IOException Thrown in case of I/O error.
     */
    protected RrdBackend doOpen(String id, boolean readOnly) throws IOException {
        if(! backends.containsKey(id)) {
            backends.put(id, new ByteBuffer());            
        }
        return new RrdMemoryBackend(id, backends.get(id));
    }

    /**
     * Method to determine if a memory storage with the given ID already exists.
     *
     * @param id Memory storage ID.
     * @return True, if such storage exists, false otherwise.
     */
    public boolean exists(String id) {
        return backends.containsKey(id);
    }

    public boolean shouldValidateHeader(String path) throws IOException {
        return false;
    }

    /**
     * Removes the storage with the given ID from the memory.
     *
     * @param id Storage ID
     * @return True, if the storage with the given ID is deleted, false otherwise.
     */
    public boolean delete(String id) {
        if (backends.containsKey(id)) {
            backends.remove(id);
            return true;
        }
        else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#getStats()
     */
    @Override
    public Map<String, Number> getStats() {
        long size = 0;
        for(ByteBuffer bb: backends.values()) {
            size += bb.buffer.length;
        }
        return Collections.singletonMap("memory usage", (Number) new Long(size));
    }

    @Override
    public String resolveUniqId(Object id) throws IOException {
        return id.toString();
    }

}
