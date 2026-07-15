function openImage(url){

    document.getElementById("overlay").style.display="flex";
    document.getElementById("bigImage").src=url;

}

function closeImage(){

    document.getElementById("overlay").style.display="none";

}

async function downloadZip(){

    const btn=document.getElementById("downloadBtn");
    const status=document.getElementById("downloadStatus");
    const text=document.querySelector(".download-text");

    btn.disabled=true;
    btn.innerText="Hazırlanıyor...";
    status.style.display="block";
    text.innerText="ZIP hazırlanıyor...";

    try{

        const response=await fetch("/album/download?id="+encodeURIComponent(folderId));

        if(!response.ok){

            throw new Error("İndirme başarısız.");

        }

        const blob=await response.blob();

        const url=window.URL.createObjectURL(blob);

        const a=document.createElement("a");

        a.href=url;

        a.download=folderId+".zip";

        document.body.appendChild(a);

        a.click();

        a.remove();

        window.URL.revokeObjectURL(url);

        text.innerText="✅ İndirme başlatıldı.";

    }

    catch(e){

        text.innerText="❌ Bir hata oluştu.";

        console.error(e);

    }

    finally{

        setTimeout(()=>{

            status.style.display="none";

            btn.disabled=false;

            btn.innerHTML="⬇️ Tüm Fotoğrafları İndir";

        },1000);

    }

}