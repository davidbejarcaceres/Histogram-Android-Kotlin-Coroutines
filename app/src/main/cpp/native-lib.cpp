#include <jni.h>
#include <string>
#include <unistd.h> // Used to sleep the thread 5 secons
#include <android/log.h>
#include <chrono>
#include <vector>
#include <thread>
#include <omp.h>


using namespace std;


//Can only be called from the MainActivity
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

// Can only be called from the Native Fragment (Second)
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    sleep(3); //Calls this to sleep the thread for 5 seconds

    return env->NewStringUTF(hello.c_str());
}

// Single Thread Histogram calculation
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_calcHistogramC(
        JNIEnv *env,
        jobject /* this */,
        jint tam,
        jshortArray imagen_,
        jintArray h_) {

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);
    jboolean isCopyA, isCopyB;
    jlong aux = 0;
    if (imagen == NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {
            jlong aux = 0;
            for (int i = 0; i < 256; i++) {// Inicializa el histograma.
                for (int k = 0; k < 3; k++) h[k + i] = 0;
            }
            for (int i = 0; i < tam; i++) {// Inicializa la imagen.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
                }
            }

            for (int i = 0; i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
                }
            }

            for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) {
                        for (int x = 0; x < 256; x++) {
                            aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] - h[k + x] * h[k + x] * h[k + x] + h[k + x] * h[k + x]);
                            h[k + x] = (int) (aux % 256);
                        }
                        imagen[k + i + j] = (short) (imagen[k + i + j] *h[k + imagen[k + i + j]]);
                    }
                }
            }
        }

        __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
        if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
        if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);
    }

    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


void histoChunk(int me, int nth, int tam, short* imagen, int* h) {
    long aux = 0;
    for (int i = 0; i < 256; i++) {// Inicializa el histograma.
        for (int k = 0; k < 3; k++) h[k + i] = 0;
    }
    for (int i = 0; i < tam; i++) {// Inicializa la imagen.
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
        }
    }
    for (int i = 0; i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
        }
    }
    for (int i = (me * tam) / nth; i < ((me + 1) * tam) / nth; i++) { // Modificar imagen utilizando el histograma
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) {
                for (int x = 0; x < 256; x++) {
                    aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] -
                                  h[k + x] * h[k + x] * h[k + x] + h[k + x] * h[k + x]);
                    h[k + x] = (int) (aux % 256);
                }
                imagen[k + i + j] = (short) (imagen[k + i + j] * h[k + imagen[k + i + j]]);
            }
        }
    }
}

// Multi Thread Histogram calculation
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_histogramaCpth(
        JNIEnv *env,
        jobject /* this */,
        jint tam,
        jint nth,
        jshortArray imagen_,
        jintArray h_) {

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);
    int** hpriv;
    jboolean isCopyA, isCopyB;
    vector<thread>t;
    hpriv=new int*[nth];
    for(int i=0; i<nth; i++){
        hpriv[i]=new int[tam];
        for(int j=0; j<tam;j++)hpriv[i][j]=0;
    }
    if (imagen==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {

            for (int i = 0; i < nth; i++)
                t.push_back(thread(histoChunk,i,nth,tam,imagen,hpriv[i]));
            for (int i = 0; i < nth; i++) {
                t[i].join();
                for(int j=0; j<256; j++) h[j]+=hpriv[i][j];
            }
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
    if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
    if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);

    std::string terminado = "DONE! Multi-Tread C++";
    return env->NewStringUTF(terminado.c_str());
}






// Multi-Thread using OpenMP
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_histogramaOpenMP(
        JNIEnv *env,
        jobject /* this */,
        jint tam,
        jshortArray imagen_,
        jintArray h_) {

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);

    jboolean isCopyA, isCopyB;
    if (imagen==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {
            int tid;
            omp_set_num_threads(8);
            #pragma omp parallel num_threads(8) private(tid)
            {omp_set_num_threads(8);
            tid = omp_get_thread_num();
            printf("Thread = %d\n", tid);

            long aux = 0;
                for (int i = 0; i < 256; i++) {// Inicializa el histograma.
                    for (int k = 0; k < 3; k++) h[k + i] = 0;
                }
                for (int i = 0; i < tam; i++) {// Inicializa la imagen.
                    for (int j = 0; j < tam; j++) {
                        for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
                    }
                }
                #pragma omp critical
                {
                    for (int i = 0;
                         i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                        for (int j = 0; j < tam; j++) {
                            for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
                        }
                    }
                }
                #pragma omp for nowait
                {
                    for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                        for (int j = 0; j < tam; j++) {
                            for (int k = 0; k < 3; k++) {
                                for (int x = 0; x < 256; x++) {
                                    aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] -
                                                  h[k + x] * h[k + x] * h[k + x] +
                                                  h[k + x] * h[k + x]);
                                    h[k + x] = (int) (aux % 256);
                                }
                                imagen[k + i + j] = (short) (imagen[k + i + j] *
                                                             h[k + imagen[k + i + j]]);
                            }
                        }
                    }
                }
            }
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
    if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
    if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);
    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

