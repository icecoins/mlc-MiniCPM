MODEL_NAME=vis-minicpm
QUANTIZATION=q4f16_1
mlc_chat convert_weight --model-type vis_minicpm ./dist/models/${MODEL_NAME}-hf/ --quantization $QUANTIZATION -o dist/$MODEL_NAME/
mlc_chat gen_config --model-type vis_minicpm ./dist/models/${MODEL_NAME}-hf/ --quantization $QUANTIZATION --conv-template LM --sliding-window-size 768 -o dist/${MODEL_NAME}/
mlc_chat compile --model-type vis_minicpm dist/${MODEL_NAME}/mlc-chat-config.json --device android -o ./dist/libs/${MODEL_NAME}-android.tar
cd ./android/library
./prepare_libs.sh
cd -

MODEL_NAME=minicpm
QUANTIZATION=q4f16_1
mlc_chat convert_weight --model-type minicpm ./dist/models/${MODEL_NAME}-gptq-hf/ --quantization $QUANTIZATION -o dist/$MODEL_NAME/
mlc_chat gen_config --model-type minicpm ./dist/models/${MODEL_NAME}-gptq-hf/ --quantization $QUANTIZATION --conv-template LM --sliding-window-size 768 -o dist/${MODEL_NAME}/
mlc_chat compile --model-type minicpm dist/${MODEL_NAME}/mlc-chat-config.json --device android -o ./dist/libs/${MODEL_NAME}-android.tar
cd ./android/library
./prepare_libs.sh
cd -

mkdir -p build && cd build
# generate build configuration
python3 ../cmake/gen_cmake_config.py && cd ..
# build `mlc_chat_cli`
cd build && cmake .. && cmake --build . --parallel $(nproc) && cd ..
# install
cd python && pip install -e . && cd ..