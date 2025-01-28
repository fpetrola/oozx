#import "Common/ShaderLib/GLSL150Compat.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

attributeHandler vec3 inPosition;

#if defined(HAS_COLORMAP) || (defined(HAS_LIGHTMAP) && !defined(SEPARATE_TEXCOORD))
    #define NEED_TEXCOORD1
#endif

attributeHandler vec2 inTexCoord;
attributeHandler vec2 inTexCoord2;
attributeHandler vec4 inColor;

varying vec2 texCoord1;
varying vec2 texCoord2;

varying vec4 vertColor;

void main(){
    #ifdef NEED_TEXCOORD1
        texCoord1 = inTexCoord;
    #endif

    #ifdef SEPARATE_TEXCOORD
        texCoord2 = inTexCoord2;
    #endif

    #ifdef HAS_VERTEXCOLOR
        vertColor = inColor;
    #endif

    vec4 modelSpacePos = vec4(inPosition, 1.0);
    #ifdef NUM_BONES
        Skinning_Compute(modelSpacePos);
    #endif

    gl_Position = TransformWorldViewProjection(modelSpacePos);
}