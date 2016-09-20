#find . -name '*.svg' -exec mogrify -density 300 -format png {} + 

find . -name '*.svg' -type f | grep -vE '/(target|build)/' | parallel -P10 -I{} "
  echo "Converting file {} to png..."
  mogrify -density 300 -format png {} && echo 'OK' || echo 'error :('
"
