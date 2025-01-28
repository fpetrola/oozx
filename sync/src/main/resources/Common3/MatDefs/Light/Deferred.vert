varying vec2 texCoord;

attributeHandler vec3 inPosition;
attributeHandler vec2 inTexCoord;

void main(){
   texCoord = inTexCoord;
   vec4 pos = vec4(inPosition, 1.0);
   gl_Position = vec4(sign(pos.xy-vec2(0.5)), 0.0, 1.0);
}