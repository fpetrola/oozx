#import "Common/ShaderLib/Instancing.glsllib"

attributeHandler vec3 inPosition;
attributeHandler vec3 inNormal;

varying vec3 normal;

void main(){
    gl_Position = TransformWorldViewProjection(vec4(inPosition,1.0));
    normal = inNormal;
}
