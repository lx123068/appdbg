package jmp0.app.clazz

import javassist.ClassPool
import javassist.CtClass
import jmp0.app.AndroidEnvironment
import jmp0.app.XAndroidDexClassLoader
import jmp0.app.interceptor.mtd.impl.ClassNativeInterceptor
import jmp0.app.interceptor.mtd.impl.HookMethodInterceptor
import org.apache.log4j.Logger

/**
 * use to intercept the class before or after class loaded
 */
abstract class ClassLoadedCallbackBase {

     protected fun loadToClass(what:String, replace:String, classLoader: XAndroidDexClassLoader):Class<*>{
         val clazz = ClassPool.getDefault().getCtClass(replace)
         val ba = clazz.apply {
             replaceClassName(replace,what)
         }.toBytecode()
         val ret = classLoader.xDefineClass(null,ba,0,ba.size)
         //xxxxx
         clazz.defrost()
         return ret
     }


    private val logger = Logger.getLogger(javaClass)

    /**
     * after xclassloader find the class file,you can modify the class
     * such as insert interceptor or just modify some static field
     */
     fun afterResolveClass(androidEnvironment: AndroidEnvironment,ctClass: CtClass):CtClass{
         //make hook java method possible,native function can not be hooked
         var pass = HookMethodInterceptor(androidEnvironment,ctClass).doChange()

         //erase native function access flag and insert callback
         pass =  ClassNativeInterceptor(androidEnvironment,pass).doChange()

         pass = afterResolveClassImpl(androidEnvironment,pass)
         return pass
     }

    abstract fun afterResolveClassImpl(androidEnvironment: AndroidEnvironment,ctClass: CtClass):CtClass



    /**
     * before xclassloader find the class file,you can replace or implement the class
     * if you implement the class and return
     * not call afterResolveClass
     */
    fun beforeResolveClass(androidEnvironment: AndroidEnvironment, className:String, classLoader: XAndroidDexClassLoader):Class<*>?{
        if (className == "android.support.v7.app.AppCompatActivity"){
            return  loadToClass("android.support.v7.app.AppCompatActivity","jmp0.app.clazz.android.AppCompatActivity",classLoader)
        }
        if (className=="android.provider.Settings\$Secure"){
            return loadToClass("android.provider.Settings\$Secure","jmp0.app.clazz.android.Settings\$Secure",classLoader)
        }
        return beforeResolveClassImpl(androidEnvironment,className,classLoader)
    }

    abstract fun beforeResolveClassImpl(androidEnvironment: AndroidEnvironment,className:String,classLoader: XAndroidDexClassLoader):Class<*>?

}