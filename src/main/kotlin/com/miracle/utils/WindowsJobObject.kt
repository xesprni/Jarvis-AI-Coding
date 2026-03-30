package com.miracle.utils

import com.intellij.openapi.diagnostic.Logger
import com.sun.jna.*
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.WString
import com.sun.jna.win32.StdCallLibrary

private val LOG = Logger.getInstance(WindowsJobObject::class.java)

/**
 * Windows Job Object 封装，用于管理进程树
 * 当 Job 被关闭时，Windows 会自动终止所有关联的进程
 */
class WindowsJobObject private constructor(private val jobHandle: WinNT.HANDLE) : AutoCloseable {
    
    companion object {
        private val isWindows = System.getProperty("os.name").lowercase().contains("win")
        
        /**
         * 创建一个新的 Job Object
         * @return WindowsJobObject 实例，如果不是 Windows 系统则返回 null
         */
        fun create(): WindowsJobObject? {
            if (!isWindows) return null
            
            // 检查JNA是否可用
            if (Kernel32Ex.INSTANCE == null) {
                LOG.warn("JNA is not available, WindowsJobObject creation skipped")
                return null
            }
            
            try {
                val job = Kernel32Ex.INSTANCE.CreateJobObjectW(null, null)
                if (job == null || job == WinNT.INVALID_HANDLE_VALUE) {
                    LOG.warn("Failed to create job object")
                    return null
                }
                
                // 配置 Job：关闭 Job 时自动杀掉所有进程
                val info = JOBOBJECT_EXTENDED_LIMIT_INFORMATION()
                info.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
                info.write()
                
                val success = Kernel32Ex.INSTANCE.SetInformationJobObject(
                    job,
                    JobObjectExtendedLimitInformation,
                    info.pointer,
                    info.size()
                )
                
                if (!success) {
                    LOG.warn("Failed to set job object information")
                    Kernel32.INSTANCE.CloseHandle(job)
                    return null
                }
                
                return WindowsJobObject(job)
            } catch (e: Exception) {
                LOG.warn("Failed to create WindowsJobObject", e)
                return null
            }
        }
        
        // 常量定义
        private const val JobObjectExtendedLimitInformation = 9
        private const val JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000
    }
    
    /**
     * 将进程加入 Job
     * @param process 要管理的进程
     * @return 是否成功
     */
    fun assignProcess(process: Process): Boolean {
        try {
            // 检查JNA是否可用
            if (Kernel32Ex.INSTANCE == null) {
                LOG.warn("JNA is not available, process assignment skipped")
                return false
            }
            
            val processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_ALL_ACCESS,
                false,
                process.pid().toInt()
            )
            
            if (processHandle == null || processHandle == WinNT.INVALID_HANDLE_VALUE) {
                LOG.warn("Failed to open process ${process.pid()}")
                return false
            }
            
            val success = Kernel32Ex.INSTANCE.AssignProcessToJobObject(jobHandle, processHandle)
            Kernel32.INSTANCE.CloseHandle(processHandle)
            
            if (!success) {
                LOG.warn("Failed to assign process ${process.pid()} to job")
                return false
            }
            
            LOG.info("Successfully assigned process ${process.pid()} to job")
            return true
        } catch (e: Exception) {
            LOG.warn("Failed to assign process to job", e)
            return false
        }
    }
    
    /**
     * 关闭 Job，Windows 会自动终止所有关联的进程
     */
    override fun close() {
        try {
            Kernel32.INSTANCE.CloseHandle(jobHandle)
            LOG.info("Job object closed, all processes should be terminated")
        } catch (e: Exception) {
            LOG.warn("Failed to close job object", e)
        }
    }
    
    // JNA 接口定义
    /**
     * Windows Kernel32 扩展接口，封装 Job Object 相关的 API
     */
    interface Kernel32Ex : StdCallLibrary {
        companion object {
            val INSTANCE: Kernel32Ex? = try {
                // 直接使用IntelliJ平台提供的JNA，不需要额外配置
                Native.load("kernel32", Kernel32Ex::class.java) as Kernel32Ex
            } catch (e: Exception) {
                LOG.warn("Failed to load kernel32, JNA native library may not be available", e)
                null
            }
        }
        
        fun CreateJobObjectW(lpJobAttributes: Pointer?, lpName: WString?): WinNT.HANDLE?
        
        fun SetInformationJobObject(
            hJob: WinNT.HANDLE,
            JobObjectInfoClass: Int,
            lpJobObjectInfo: Pointer,
            cbJobObjectInfoLength: Int
        ): Boolean
        
        fun AssignProcessToJobObject(
            hJob: WinNT.HANDLE,
            hProcess: WinNT.HANDLE
        ): Boolean
    }
    
    // Job Object 结构体定义
    /**
     * Job Object 基本限制信息结构体
     */
    class JOBOBJECT_BASIC_LIMIT_INFORMATION : Structure() {
        @JvmField var PerProcessUserTimeLimit = WinNT.LARGE_INTEGER()
        @JvmField var PerJobUserTimeLimit = WinNT.LARGE_INTEGER()
        @JvmField var LimitFlags = 0
        @JvmField var MinimumWorkingSetSize: Pointer? = null
        @JvmField var MaximumWorkingSetSize: Pointer? = null
        @JvmField var ActiveProcessLimit = 0
        @JvmField var Affinity: Pointer? = null
        @JvmField var PriorityClass = 0
        @JvmField var SchedulingClass = 0
        
        override fun getFieldOrder() = listOf(
            "PerProcessUserTimeLimit",
            "PerJobUserTimeLimit",
            "LimitFlags",
            "MinimumWorkingSetSize",
            "MaximumWorkingSetSize",
            "ActiveProcessLimit",
            "Affinity",
            "PriorityClass",
            "SchedulingClass"
        )
    }
    
    /**
     * IO 计数器结构体
     */
    class IO_COUNTERS : Structure() {
        @JvmField var ReadOperationCount: Long = 0
        @JvmField var WriteOperationCount: Long = 0
        @JvmField var OtherOperationCount: Long = 0
        @JvmField var ReadTransferCount: Long = 0
        @JvmField var WriteTransferCount: Long = 0
        @JvmField var OtherTransferCount: Long = 0
        
        override fun getFieldOrder() = listOf(
            "ReadOperationCount",
            "WriteOperationCount",
            "OtherOperationCount",
            "ReadTransferCount",
            "WriteTransferCount",
            "OtherTransferCount"
        )
    }
    
    /**
     * Job Object 扩展限制信息结构体
     */
    class JOBOBJECT_EXTENDED_LIMIT_INFORMATION : Structure() {
        @JvmField var BasicLimitInformation = JOBOBJECT_BASIC_LIMIT_INFORMATION()
        @JvmField var IoInfo = IO_COUNTERS()
        @JvmField var ProcessMemoryLimit: Pointer? = null
        @JvmField var JobMemoryLimit: Pointer? = null
        @JvmField var PeakProcessMemoryUsed: Pointer? = null
        @JvmField var PeakJobMemoryUsed: Pointer? = null
        
        override fun getFieldOrder() = listOf(
            "BasicLimitInformation",
            "IoInfo",
            "ProcessMemoryLimit",
            "JobMemoryLimit",
            "PeakProcessMemoryUsed",
            "PeakJobMemoryUsed"
        )
    }
}
