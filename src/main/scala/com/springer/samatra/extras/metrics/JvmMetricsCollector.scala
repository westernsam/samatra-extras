package com.springer.samatra.extras.metrics

import java.lang.Math._
import java.lang.management.{RuntimeMXBean, ThreadMXBean, _}

import com.sun.management.UnixOperatingSystemMXBean

import scala.collection.JavaConverters._
import scala.collection.mutable


class JvmMetricsCollector {
  def jvmGauges: mutable.Map[String, Number] = {
    val out: mutable.Map[String, Number] = mutable.Map[String, Number]()
    recordRuntimeMemoryUsageTo(out)
    val mem: MemoryMXBean = ManagementFactory.getMemoryMXBean
    out.put("finalizer.count", mem.getObjectPendingFinalizationCount)
    recordHeapMemoryUsageTo(out, mem)
    recordNonHeapMemoryUsageTo(out, mem)
    recordMemoryPoolUsageTo(out, "post_gc", _.getCollectionUsage)
    recordMemoryPoolUsageTo(out, "current_mem", _.getUsage)
    recordThreadInfoTo(out)
    recordUptimeTo(out)
    recordOsStatsTo(out)
    out
  }

  def jvmCounters: mutable.Map[String, Number] = {
    val out: mutable.Map[String, Number] = mutable.Map[String, Number]()
    recordGarbageCollectionTo(out, "cycles", _.getCollectionCount)
    recordGarbageCollectionTo(out, "msec", _.getCollectionTime)
    out
  }

  private def recordRuntimeMemoryUsageTo(out: mutable.Map[String, Number]) {
    val runtime: Runtime = Runtime.getRuntime
    out.put("memory.free", runtime.freeMemory)
    out.put("memory.total", runtime.totalMemory)
    out.put("memory.max", runtime.maxMemory)
    out.put("memory.available", runtime.maxMemory - (runtime.totalMemory - runtime.freeMemory))
  }
  private def recordNonHeapMemoryUsageTo(out: mutable.Map[String, Number], mem: MemoryMXBean) {
    val nonHeap: MemoryUsage = mem.getNonHeapMemoryUsage
    out.put("nonheap.committed", nonHeap.getCommitted)
    out.put("nonheap.max", nonHeap.getMax)
    out.put("nonheap.used", nonHeap.getUsed)
  }
  private def recordHeapMemoryUsageTo(out: mutable.Map[String, Number], mem: MemoryMXBean) {
    val heap: MemoryUsage = mem.getHeapMemoryUsage
    out.put("heap.committed", heap.getCommitted)
    out.put("heap.max", heap.getMax)
    out.put("heap.used", heap.getUsed)
  }
  private def recordThreadInfoTo(out: mutable.Map[String, Number]) {
    val threads: ThreadMXBean = ManagementFactory.getThreadMXBean
    out.put("thread.daemon_count", threads.getDaemonThreadCount.toLong)
    out.put("thread.count", threads.getThreadCount.toLong)
    out.put("thread.peak_count", threads.getPeakThreadCount.toLong)
  }
  private def recordUptimeTo(out: mutable.Map[String, Number]) {
    val runtime: RuntimeMXBean = ManagementFactory.getRuntimeMXBean
    out.put("start_time", runtime.getStartTime)
    out.put("uptime", runtime.getUptime)
  }
  private def recordOsStatsTo(out: mutable.Map[String, Number]) {
    val os: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean
    out.put("num_cpus", os.getAvailableProcessors.toLong)
    os match {
      case unix: UnixOperatingSystemMXBean =>
        out.put("fd.count", unix.getOpenFileDescriptorCount)
        out.put("fd.limit", unix.getMaxFileDescriptorCount)
        out.put("load_avg", unix.getSystemLoadAverage)
      case _ =>
    }
  }
  private def recordMemoryPoolUsageTo(stats: mutable.Map[String, Number], statName: String, extractor: MemoryPoolMXBean => MemoryUsage) {
    var total: Long = 0

    for (memoryPoolMXBean <- ManagementFactory.getMemoryPoolMXBeans.asScala) {
      val name: String = filterName(memoryPoolMXBean.getName)
      val usage: MemoryUsage = extractor(memoryPoolMXBean)
      if (usage != null) {
        stats.put(statName + "." + name + ".used", usage.getUsed)
        stats.put(statName + "." + name + ".max", usage.getMax)
        total += usage.getUsed
      }
    }
    stats.put(statName + ".used", total)
  }

  private def recordGarbageCollectionTo(out: mutable.Map[String, Number], statName: String, extractor: GarbageCollectorMXBean => Long) {
    var total: Long = 0L
    for (gc <- ManagementFactory.getGarbageCollectorMXBeans.asScala) {
      val collectionTime: Long = extractor(gc)
      out.put("gc." + filterName(gc.getName) + "." + statName, collectionTime)
      total += max(collectionTime, 0L)
    }
    out.put("gc." + statName, total)
  }

  private def filterName(name: String): String = name.replaceAll("[^\\w]", ".")
}
