#ifndef NOMINMAX
#define NOMINMAX
#endif

#include <windows.h>
#include <shobjidl.h>
#include <wrl.h>
#include <stdio.h>
#include "WebView2.h"

#include <dwmapi.h>
#include <audiopolicy.h>
#include <mmdeviceapi.h>
#include "WebView2EnvironmentOptions.h"

#ifndef DWMWA_USE_IMMERSIVE_DARK_MODE
#define DWMWA_USE_IMMERSIVE_DARK_MODE 20
#endif

#ifndef DWMWA_CAPTION_COLOR
#define DWMWA_CAPTION_COLOR 35
#endif

using namespace Microsoft::WRL;

static HWND g_hWnd = nullptr;
static ComPtr<ICoreWebView2Controller> g_webViewController;
static ComPtr<ICoreWebView2> g_webView;

void SetAudioSessionMetadata(const wchar_t* exePath) {
    ComPtr<IMMDeviceEnumerator> enumerator;
    if (SUCCEEDED(CoCreateInstance(__uuidof(MMDeviceEnumerator), NULL, CLSCTX_ALL, IID_PPV_ARGS(&enumerator)))) {
        ComPtr<IMMDevice> device;
        if (SUCCEEDED(enumerator->GetDefaultAudioEndpoint(eRender, eMultimedia, &device))) {
            ComPtr<IAudioSessionManager2> sessionManager;
            if (SUCCEEDED(device->Activate(__uuidof(IAudioSessionManager2), CLSCTX_ALL, NULL, &sessionManager))) {
                wchar_t iconPath[MAX_PATH];
                swprintf_s(iconPath, MAX_PATH, L"%s\\RayMusicApp.exe,-1", exePath);

                GUID rayMusicSessionGuid = { 0x5a23f991, 0x3d7b, 0x48e2, { 0x9b, 0x14, 0x1f, 0xe8, 0x94, 0xcb, 0x22, 0x11 } };

                ComPtr<IAudioSessionControl> sessionControl;
                if (SUCCEEDED(sessionManager->GetAudioSessionControl(&rayMusicSessionGuid, 0, &sessionControl))) {
                    sessionControl->SetDisplayName(L"RayMusic", NULL);
                    sessionControl->SetIconPath(iconPath, NULL);
                }

                ComPtr<IAudioSessionEnumerator> sessionEnum;
                if (SUCCEEDED(sessionManager->GetSessionEnumerator(&sessionEnum)) && sessionEnum) {
                    int count = 0;
                    sessionEnum->GetCount(&count);
                    bool primarySet = false;
                    for (int i = 0; i < count; ++i) {
                        ComPtr<IAudioSessionControl> sessionCtrl;
                        if (SUCCEEDED(sessionEnum->GetSession(i, &sessionCtrl)) && sessionCtrl) {
                            ComPtr<IAudioSessionControl2> sessionCtrl2;
                            if (SUCCEEDED(sessionCtrl.As(&sessionCtrl2)) && sessionCtrl2) {
                                sessionCtrl2->SetGroupingParam(&rayMusicSessionGuid, NULL);
                                if (!primarySet) {
                                    sessionCtrl2->SetDisplayName(L"RayMusic", NULL);
                                    sessionCtrl2->SetIconPath(iconPath, NULL);
                                    primarySet = true;
                                } else {
                                    sessionCtrl2->SetDisplayName(L"", NULL);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
    case WM_SIZE:
        if (g_webViewController) {
            RECT bounds;
            GetClientRect(hWnd, &bounds);
            g_webViewController->put_Bounds(bounds);
        }
        break;
    case WM_SETFOCUS:
        if (g_webViewController) {
            g_webViewController->MoveFocus(COREWEBVIEW2_MOVE_FOCUS_REASON_PROGRAMMATIC);
        }
        break;
    case WM_DESTROY:
        PostQuitMessage(0);
        break;
    default:
        return DefWindowProc(hWnd, message, wParam, lParam);
    }
    return 0;
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);

    SetCurrentProcessExplicitAppUserModelID(L"RayMusic");

    HICON hIconBig = (HICON)LoadImageW(hInstance, MAKEINTRESOURCEW(1), IMAGE_ICON, 256, 256, LR_SHARED);
    HICON hIconSm = (HICON)LoadImageW(hInstance, MAKEINTRESOURCEW(1), IMAGE_ICON, 32, 32, LR_SHARED);

    WNDCLASSEXW wcex = { sizeof(WNDCLASSEX) };
    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = WndProc;
    wcex.hInstance = hInstance;
    wcex.hCursor = LoadCursor(nullptr, IDC_ARROW);
    wcex.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
    wcex.lpszClassName = L"RayMusicAppClass";
    wcex.hIcon = hIconBig ? hIconBig : LoadIcon(hInstance, IDI_APPLICATION);
    wcex.hIconSm = hIconSm ? hIconSm : LoadIcon(hInstance, IDI_APPLICATION);

    RegisterClassExW(&wcex);

    HWND hWnd = CreateWindowExW(
        WS_EX_APPWINDOW,
        L"RayMusicAppClass",
        L"RayMusic",
        WS_OVERLAPPEDWINDOW,
        CW_USEDEFAULT, CW_USEDEFAULT,
        1280, 800,
        nullptr, nullptr, hInstance, nullptr
    );

    if (!hWnd) return 1;

    // Apply dark titlebar (#0a0a0c) to match app background
    BOOL useDarkMode = TRUE;
    DwmSetWindowAttribute(hWnd, DWMWA_USE_IMMERSIVE_DARK_MODE, &useDarkMode, sizeof(useDarkMode));
    COLORREF captionColor = RGB(10, 10, 12);
    DwmSetWindowAttribute(hWnd, DWMWA_CAPTION_COLOR, &captionColor, sizeof(captionColor));

    if (hIconBig) {
        SendMessageW(hWnd, WM_SETICON, ICON_BIG, (LPARAM)hIconBig);
    }
    if (hIconSm) {
        SendMessageW(hWnd, WM_SETICON, ICON_SMALL, (LPARAM)hIconSm);
    }

    g_hWnd = hWnd;
    ShowWindow(hWnd, nCmdShow);
    UpdateWindow(hWnd);

    wchar_t exePath[MAX_PATH];
    GetModuleFileNameW(NULL, exePath, MAX_PATH);
    wchar_t* lastSlash = wcsrchr(exePath, L'\\');
    if (lastSlash) *lastSlash = L'\0';

    wchar_t htmlPath[MAX_PATH];
    swprintf_s(htmlPath, MAX_PATH, L"file:///%s/src/index.html", exePath);
    for (int i = 0; htmlPath[i] != L'\0'; ++i) {
        if (htmlPath[i] == L'\\') htmlPath[i] = L'/';
    }

    wchar_t userDataFolder[MAX_PATH];
    swprintf_s(userDataFolder, MAX_PATH, L"%s\\RayMusicUserData", exePath);

    SetAudioSessionMetadata(exePath);

    auto options = Make<CoreWebView2EnvironmentOptions>();
    if (options) {
        options->put_AdditionalBrowserArguments(L"--disable-web-security --allow-file-access-from-files --autoplay-policy=no-user-gesture-required --no-user-gesture-required --enable-features=AutoplayIgnoreWebAudio --app-name=RayMusic");
    }

    HRESULT hrEnv = CreateCoreWebView2EnvironmentWithOptions(
        nullptr, userDataFolder, options ? options.Get() : nullptr,
        Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
            [htmlPath, hWnd, exePath](HRESULT result, ICoreWebView2Environment* env) -> HRESULT {
                if (FAILED(result) || !env) {
                    wchar_t errBuf[256];
                    swprintf_s(errBuf, 256, L"Error al crear el entorno WebView2. HRESULT: 0x%08X\nAsegúrate de tener Microsoft Edge WebView2 instalado.", result);
                    MessageBoxW(hWnd, errBuf, L"RayMusic - Error de Inicialización", MB_OK | MB_ICONERROR);
                    return S_OK;
                }

                env->CreateCoreWebView2Controller(hWnd,
                    Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
                        [htmlPath, hWnd, exePath](HRESULT result, ICoreWebView2Controller* controller) -> HRESULT {
                            if (FAILED(result) || !controller) {
                                wchar_t errBuf[256];
                                swprintf_s(errBuf, 256, L"Error al crear el controlador WebView2. HRESULT: 0x%08X", result);
                                MessageBoxW(hWnd, errBuf, L"RayMusic - Error de Inicialización", MB_OK | MB_ICONERROR);
                                return S_OK;
                            }

                            g_webViewController = controller;
                            g_webViewController->put_IsVisible(TRUE);
                            g_webViewController->get_CoreWebView2(&g_webView);

                            RECT bounds;
                            GetClientRect(hWnd, &bounds);
                            g_webViewController->put_Bounds(bounds);

                            ComPtr<ICoreWebView2Settings> settings;
                            if (SUCCEEDED(g_webView->get_Settings(&settings)) && settings) {
                                settings->put_IsScriptEnabled(TRUE);
                                settings->put_AreDefaultScriptDialogsEnabled(TRUE);
                                settings->put_IsWebMessageEnabled(TRUE);
                                settings->put_IsStatusBarEnabled(FALSE);
                                settings->put_AreDevToolsEnabled(TRUE);
                            }

                            ComPtr<ICoreWebView2_8> webView8;
                            if (SUCCEEDED(g_webView.As(&webView8)) && webView8) {
                                webView8->put_IsMuted(FALSE);
                            }

                            // Set Virtual Host Name Mapping for secure https://raymusic.app origin
                            ComPtr<ICoreWebView2_3> webView3;
                            if (SUCCEEDED(g_webView.As(&webView3)) && webView3) {
                                webView3->SetVirtualHostNameToFolderMapping(
                                    L"raymusic.app",
                                    exePath,
                                    COREWEBVIEW2_HOST_RESOURCE_ACCESS_KIND_ALLOW
                                );

                                // Add WebResourceRequested filter for Apple Music API to bypass CORS origin restrictions
                                g_webView->AddWebResourceRequestedFilter(L"https://amp-api.music.apple.com/*", COREWEBVIEW2_WEB_RESOURCE_CONTEXT_ALL);
                                g_webView->AddWebResourceRequestedFilter(L"https://beta.music.apple.com/*", COREWEBVIEW2_WEB_RESOURCE_CONTEXT_ALL);
                                g_webView->AddWebResourceRequestedFilter(L"https://*.itunes.apple.com/*", COREWEBVIEW2_WEB_RESOURCE_CONTEXT_ALL);

                                EventRegistrationToken resToken;
                                g_webView->add_WebResourceRequested(
                                    Callback<ICoreWebView2WebResourceRequestedEventHandler>(
                                        [](ICoreWebView2* sender, ICoreWebView2WebResourceRequestedEventArgs* args) -> HRESULT {
                                            ComPtr<ICoreWebView2WebResourceRequest> req;
                                            if (SUCCEEDED(args->get_Request(&req)) && req) {
                                                ComPtr<ICoreWebView2HttpRequestHeaders> headers;
                                                if (SUCCEEDED(req->get_Headers(&headers)) && headers) {
                                                    headers->SetHeader(L"Origin", L"https://music.apple.com");
                                                    headers->SetHeader(L"Referer", L"https://music.apple.com/");
                                                }
                                            }
                                            return S_OK;
                                        }).Get(), &resToken);

                                // Refresh WASAPI Audio Session metadata after NavigationCompleted
                                EventRegistrationToken navToken;
                                g_webView->add_NavigationCompleted(
                                    Callback<ICoreWebView2NavigationCompletedEventHandler>(
                                        [exePath](ICoreWebView2* sender, ICoreWebView2NavigationCompletedEventArgs* args) -> HRESULT {
                                            SetAudioSessionMetadata(exePath);
                                            return S_OK;
                                        }).Get(), &navToken);

                                g_webView->Navigate(L"https://raymusic.app/src/index.html");
                            } else {
                                g_webView->Navigate(htmlPath);
                            }
                            return S_OK;
                        }).Get());
                return S_OK;
            }).Get());

    if (FAILED(hrEnv)) {
        wchar_t errBuf[256];
        swprintf_s(errBuf, 256, L"CreateCoreWebView2EnvironmentWithOptions falló. HRESULT: 0x%08X", hrEnv);
        MessageBoxW(hWnd, errBuf, L"RayMusic - Error", MB_OK | MB_ICONERROR);
    }

    MSG msg;
    while (GetMessage(&msg, nullptr, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }

    return (int)msg.wParam;
}
