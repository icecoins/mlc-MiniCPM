MODEL_NAME=CPM-2B
QUANTIZATION=q4f16_1
mlc_chat convert_weight --model-type mistral ./dist/models/${MODEL_NAME}-hf/ --quantization $QUANTIZATION -o dist/$MODEL_NAME-$QUANTIZATION/
mlc_chat gen_config --model-type mistral ./dist/models/${MODEL_NAME}-hf/ --quantization $QUANTIZATION --conv-template LM --sliding-window-size 768 -o dist/${MODEL_NAME}-${QUANTIZATION}/
mlc_chat compile --model-type mistral ./dist/${MODEL_NAME}-${QUANTIZATION}/mlc-chat-config.json --device android -o ./dist/libs/${MODEL_NAME}-${QUANTIZATION}-android.tar
cd ./android/library
./prepare_libs.sh
<!-- mlc_chat compile --model-type mistral ./dist/${MODEL_NAME}-${QUANTIZATION}/mlc-chat-config.json --device iphone -o ./dist/libs/${MODEL_NAME}-${QUANTIZATION}-iphone.tar -->
<!-- mlc_chat compile ./dist/${MODEL_NAME}-${QUANTIZATION}-MLC/mlc-chat-config.json --device cuda -o dist/libs/${MODEL_NAME}-${QUANTIZATION}-cuda.so -->

MODEL_NAME=CPM-2B-sft
QUANTIZATION=q4f16_1
mlc_chat convert_weight --model-type mistral ./dist/models/${MODEL_NAME}-hf/ --quantization $QUANTIZATION -o dist/$MODEL_NAME-$QUANTIZATION/
mlc_chat gen_config --model-type mistral ./dist/models/${MODEL_NAME}-hf/ --quantization $QUANTIZATION --conv-template LM --sliding-window-size 768 -o dist/${MODEL_NAME}-${QUANTIZATION}/
mlc_chat compile --model-type mistral dist/${MODEL_NAME}-${QUANTIZATION}/mlc-chat-config.json --device android -o ./dist/libs/${MODEL_NAME}-${QUANTIZATION}-android.tar
cd ./android/library
./prepare_libs.sh

MODEL_NAME=Mistral-7b-hf
QUANTIZATION=q4f16_1
mlc_chat convert_weight ./dist/models/${MODEL_NAME}/ --quantization $QUANTIZATION -o dist/$MODEL_NAME-$QUANTIZATION/
mlc_chat gen_config ./dist/models/${MODEL_NAME}/ --quantization $QUANTIZATION --conv-template LM --sliding-window-size 768 -o dist/${MODEL_NAME}-${QUANTIZATION}/
mlc_chat compile ./dist/${MODEL_NAME}-${QUANTIZATION}/mlc-chat-config.json --device android -o ./dist/libs/${MODEL_NAME}-${QUANTIZATION}-android.tar

MODEL_NAME=RedPajama-INCITE-Chat-3B-v1-hf
QUANTIZATION=q4f16_1
mlc_chat convert_weight ./dist/models/${MODEL_NAME}/ --quantization $QUANTIZATION -o dist/$MODEL_NAME-$QUANTIZATION-MLC/
mlc_chat gen_config ./dist/models/${MODEL_NAME}/ --quantization $QUANTIZATION --conv-template LM --context-window-size 768 -o dist/${MODEL_NAME}-${QUANTIZATION}-MLC/
mlc_chat compile ./dist/${MODEL_NAME}-${QUANTIZATION}-MLC/mlc-chat-config.json --device android -o ./dist/libs/${MODEL_NAME}-${QUANTIZATION}-android.tar