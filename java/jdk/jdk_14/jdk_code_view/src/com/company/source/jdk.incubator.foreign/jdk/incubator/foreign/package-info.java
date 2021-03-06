/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/**
 * <p> Classes to support low-level, safe and efficient memory access. For example:
 *
 * <pre>{@code
static final VarHandle intHandle = MemoryHandles.varHandle(int.class, ByteOrder.BIG_ENDIAN);

try (MemorySegment segment = MemorySegment.allocateNative(10 * 4)) {
   MemoryAddress base = segment.baseAddress();
   for (long i = 0 ; i < 10 ; i++) {
     intHandle.set(base.addOffset(i * 4), (int)i);
   }
 }
 * }</pre>
 *
 * Here we create a var handle, namely {@code intHandle}, to manipulate values of the primitive type {@code int}, at
 * a given memory location. We then create a <em>native</em> memory segment, that is, a memory segment backed by
 * off-heap memory; the size of the segment is 40 bytes, enough to store 10 values of the primitive type {@code int}.
 * The segment is created inside a <em>try-with-resources</em> construct: this idiom ensures that all the memory resources
 * associated with the segment will be released at the end of the block. Inside the try-with-resources block, we initialize
 * the contents of the memory segment; more specifically, if we view the memory segment as a set of 10 adjacent slots,
 * {@code s[i]}, where {@code 0 <= i < 10}, where the size of each slot is exactly 4 bytes, the initialization logic above will set each slot
 * so that {@code s[i] = i}, again where {@code 0 <= i < 10}.
 *
 * <p>
 * The key abstractions introduced by this package are {@link jdk.incubator.foreign.MemorySegment} and {@link jdk.incubator.foreign.MemoryAddress}.
 * The first models a contiguous memory region, which can reside either inside or outside the Java heap; the latter models an address - that is,
 * an offset inside a given segment. A memory address represents the main access coordinate of a memory access var handle, which can be obtained
 * using the combinator methods defined in the {@link jdk.incubator.foreign.MemoryHandles} class. Finally, the {@link jdk.incubator.foreign.MemoryLayout} class
 * hierarchy enables description of <em>memory layouts</em> and basic operations such as computing the size in bytes of a given
 * layout, obtain its alignment requirements, and so on. Memory layouts also provide an alternate, more abstract way, to produce
 * memory access var handles, e.g. using <a href="MemoryLayout.html#layout-paths"><em>layout paths</em></a>.
 *
 * <h2><a id="deallocation"></a>Deterministic deallocation</h2>
 *
 * When writing code that manipulates memory segments, especially if backed by memory which resides outside the Java heap, it is
 * crucial that the resources associated with a memory segment are released when the segment is no longer in use, by calling the {@link jdk.incubator.foreign.MemorySegment#close()}
 * method either explicitly, or implicitly, by relying on try-with-resources construct (as demonstrated in the example above).
 * Closing a given memory segment is an <em>atomic</em> operation which can either succeed - and result in the underlying
 * memory associated with the segment to be released, or <em>fail</em> with an exception.
 * <p>
 * The deterministic deallocation model differs significantly from the implicit strategies adopted within other APIs, most
 * notably the {@link java.nio.ByteBuffer} API: in that case, when a native byte buffer is created (see {@link java.nio.ByteBuffer#allocateDirect(int)}),
 * the underlying memory is not released until the byte buffer reference becomes <em>unreachable</em>. While implicit deallocation
 * models such as this can be very convenient - clients do not have to remember to <em>close</em> a direct buffer - such models can also make it
 * hard for clients to ensure that the memory associated with a direct buffer has indeed been released.
 *
 * <h2><a id="safety"></a>Safety</h2>
 *
 * This API provides strong safety guarantees when it comes to memory access. First, when dereferencing a memory segment using
 * a memory address, such an address is validated (upon access), to make sure that it does not point to a memory location
 * which resides <em>outside</em> the boundaries of the memory segment it refers to. We call this guarantee <em>spatial safety</em>.
 * <p>
 * Since memory segments can be closed (see above), a memory address is also validated (upon access) to make sure that
 * the segment it belongs to has not been closed prematurely. We call this guarantee <em>temporal safety</em>. Note that,
 * in the general case, guaranteeing temporal safety can be hard, as multiple threads could attempt to access and/or close
 * the same memory segment concurrently. The memory access API addresses this problem by imposing strong
 * <a href="MemorySegment.html#thread-confinement"><em>thread-confinement</em></a> guarantees on memory segments: each
 * memory segment is associated with an owner thread, which is the only thread that can either access or close the segment.
 * A thread other than the owner thread will have to explicitly <em>acquire</em> a segment in order to be able to use it.
 * <p>
 * Together, spatial and temporal safety ensure that each memory access operation either succeeds - and accesses a valid
 * memory location - or fails.
 */
package jdk.incubator.foreign;
