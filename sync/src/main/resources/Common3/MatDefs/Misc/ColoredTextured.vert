uniform mat4 g_WorldViewProjectionMatrix;

attributeHandler vec3 inPosition;
attributeHandler vec2 inTexCoord;

varying vec2 texCoord;

void main(){
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
    texCoord = inTexCoord;
}