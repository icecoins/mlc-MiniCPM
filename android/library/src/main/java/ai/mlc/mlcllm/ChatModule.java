package ai.mlc.mlcllm;

import android.nfc.tech.Ndef;

import org.apache.tvm.Device;
import org.apache.tvm.Function;
import org.apache.tvm.Module;
import org.apache.tvm.NDArrayBase;
import org.apache.tvm.NDArray;
import org.apache.tvm.TVMType;

public class ChatModule {
    private Function reloadFunc;
    private Function unloadFunc;
    private Function prefillFunc;
    private Function imageFunc;
    private Function decodeFunc;
    private Function getMessage;
    private Function stoppedFunc;
    private Function resetChatFunc;
    private Function runtimeStatsTextFunc;
    private Module llmChat;

    public ChatModule() {
        Function createFunc = Function.getFunction("mlc.llm_chat_create");
        assert createFunc != null;
        llmChat = createFunc.pushArg(Device.opencl().deviceType).pushArg(0).invoke().asModule();
        reloadFunc = llmChat.getFunction("reload");
        unloadFunc = llmChat.getFunction("unload");
        prefillFunc = llmChat.getFunction("prefill");
        imageFunc = llmChat.getFunction("image");
        decodeFunc = llmChat.getFunction("decode");
        getMessage = llmChat.getFunction("get_message");
        stoppedFunc = llmChat.getFunction("stopped");
        resetChatFunc = llmChat.getFunction("reset_chat");
        runtimeStatsTextFunc = llmChat.getFunction("runtime_stats_text");
    }

    public void unload() {
        unloadFunc.invoke();
    }

    public void reload(String modelLib, String modelPath) {
        String libPrefix = modelLib.replace('-', '_') + "_";
        Function systemLibFunc = Function.getFunction("runtime.SystemLib");
        assert systemLibFunc != null;
        systemLibFunc = systemLibFunc.pushArg(libPrefix);
        Module lib = systemLibFunc.invoke().asModule();
        reloadFunc = reloadFunc.pushArg(lib).pushArg(modelPath);
        reloadFunc.invoke();
    }

    public void resetChat() {
        resetChatFunc.invoke();
    }

    public void prefill(String input) {
        prefillFunc.pushArg(input).invoke();
    }

    public void image() {
        int C = 3, H = 224, W = 224;
        long[] shape = {1, C, H, W};
        NDArray img = NDArray.empty(shape, new TVMType("int32"));
        int[] inp = new int[C * H * W];
        for (int i = 0; i < C * H * W; ++i) {
            if (i % 3 == 0) inp[i] = 0;
            if (i % 3 == 1) inp[i] = 0;
            if (i % 3 == 2) inp[i] = 0;
        }
        img.copyFrom(inp);
        NDArrayBase res = imageFunc.pushArg(img).invoke().asNDArray();

        long[] res_shape = {1, 256, 1152};
//        long[] res_shape = {1, 256, 4304};
//        long[] res_shape = {1, 1152};
        NDArray arr = NDArray.empty(res_shape, new TVMType("float32"));
        res.copyTo(arr);
        float[] farr = arr.asFloatArray();
        float[] sum = new float[256];
        float[] repr = new float[6*6];
        for (int i = 0; i < 256; ++i) {
            sum[i] = 0;
            for (int j = 0; j < 1152; ++j) {
                sum[i] += farr[i * 1152 + j];
                if (i < 3) {
                    if (j < 3) repr[i*6+j] = farr[i * 1152 + j];
                    if (1152 - j <= 3) repr[i*6+j-1149+3] = farr[i * 1152 + j];
                }
                if (256 - i <= 3) {
                    if (j < 3) repr[3*6+(i-253)*6+j] = farr[i * 1152 + j];
                    if (1152 - j <= 3) repr[3*6+(i-253)*6+j-1149+3] = farr[i * 1152 + j];
                }
            }
        }

        String s = "";
    }

    public String getMessage() {
        return getMessage.invoke().asString();
    }

    public String runtimeStatsText() {
        return runtimeStatsTextFunc.invoke().asString();
    }

    public void evaluate() {
        llmChat.getFunction("evaluate").invoke();
    }

    public boolean stopped() {
        return stoppedFunc.invoke().asLong() != 0L;
    }

    public void decode() {
        decodeFunc.invoke();
    }
}