package com.aliyun.code83.round2.test.beams;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.Credentials;
import com.aliyun.fc.runtime.FunctionComputeLogger;
import com.aliyun.fc.runtime.FunctionParam;

public class MockContextImpl implements Context {
    @Override
    public String getRequestId() {
        return "mock";
    }

    @Override
    public Credentials getExecutionCredentials() {
        return null;
    }

    @Override
    public FunctionParam getFunctionParam() {
        return null;
    }

    @Override
    public FunctionComputeLogger getLogger() {
        return new FunctionComputeLoggerImpl();
    }

    static class FunctionComputeLoggerImpl implements FunctionComputeLogger {

        @Override
        public void trace(String s) {
            System.out.println(s);
        }

        @Override
        public void debug(String s) {
            System.out.println(s);
        }

        @Override
        public void info(String s) {
            System.out.println(s);
        }

        @Override
        public void warn(String s) {
            System.out.println(s);
        }

        @Override
        public void error(String s) {
            System.out.println(s);
        }

        @Override
        public void fatal(String s) {
            System.out.println(s);
        }

        @Override
        public void setLogLevel(Level level) {

        }
    }
}
