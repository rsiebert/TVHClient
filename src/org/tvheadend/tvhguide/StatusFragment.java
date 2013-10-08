package org.tvheadend.tvhguide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatusFragment extends Fragment implements HTSListener {

	private TextView freediscspace;
	private TextView totaldiscspace;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null)
            return null;

        View v = inflater.inflate(R.layout.status_layout, container, false);
        freediscspace = (TextView) v.findViewById(R.id.free_discspace);
        totaldiscspace = (TextView) v.findViewById(R.id.total_discspace);
        return v;
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
	}
	
    @Override
    public void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.addListener(this);

        Intent intent = new Intent(getActivity(), HTSService.class);
        intent.setAction(HTSService.ACTION_GET_DISC_STATUS);
        getActivity().startService(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.removeListener(this);
    }

	@Override
	public void onMessage(final String action, final Object obj) {
		
		if (action.equals(TVHGuideApplication.ACTION_STATUS)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                	
                	@SuppressWarnings("unchecked")
					HashMap<String, String> list = (HashMap<String, String>) obj;
                	
                	// Set the values
                	freediscspace.setText(list.get("freediscspace"));
                	totaldiscspace.setText(list.get("totaldiscspace"));
                }
            });
        }
	}

}
